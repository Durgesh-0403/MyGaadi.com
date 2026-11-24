package com.mygaadi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.custom_exceptions.ConflictException;
import com.mygaadi.custom_exceptions.InvalidInputException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.BookingDTO;
import com.mygaadi.dtos.InternalBookingDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.Booking;
import com.mygaadi.entities.BookingStatus;
import com.mygaadi.entities.Car;
import com.mygaadi.entities.CarStatus;
import com.mygaadi.entities.PaymentStatus;
import com.mygaadi.entities.User;
import com.mygaadi.repository.BookingRepository;
import com.mygaadi.repository.CarRepository;
import com.mygaadi.repository.UserRepository;
import com.mygaadi.service.client.PaymentClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

	@Value("${booking.token.percent:2.0}")
	private double tokenPercent;

	@Value("${booking.token.min-amount:5000.00}")
	private BigDecimal minTokenAmount;

	@Value("${booking.token.max-amount:50000.00}")
	private BigDecimal maxTokenAmount;

	@Value("${booking.pending.ttl-minutes:15}")
	private int ttlMinutes;

	@Value("${internal.api.key}")
	private String internalApiKey;

	private final BookingRepository bookingRepository;
	private final CarRepository carRepository;
	private final UserRepository userRepository;
	private final PaymentClient paymentClient;

	@Override
	@Transactional
	public BookingDTO createBooking(Long buyerId, Long carId) {
		User buyer = userRepository.findById(buyerId)
				.orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + buyerId));

		if (!buyer.isActive()) {
			throw new AccessDeniedException("Your account is blocked. You cannot book cars.");
		}

		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (car.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new ConflictException("Car is not approved for sale");
		}

		if (car.getStatus() == CarStatus.SOLD) {
			throw new ConflictException("This car has already been sold and is no longer available for booking.");
		}

		// A single buyer can only book the same car once while active
		boolean alreadyBookedByBuyer = bookingRepository.existsByBuyerIdAndCarIdAndStatusIn(
				buyerId,
				carId,
				List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED)
		);
		if (alreadyBookedByBuyer) {
			throw new ConflictException("You have already booked this car. Please view your reservation in 'My Bookings'.");
		}

		// Calculate token amount
		BigDecimal percent = BigDecimal.valueOf(tokenPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
		BigDecimal calculatedAmount = car.getPrice().multiply(percent).setScale(2, RoundingMode.HALF_UP);
		BigDecimal tokenAmount = calculatedAmount.max(minTokenAmount).min(maxTokenAmount);

		String bookingNumber = "BKG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);

		// Multiple bookings can be done and shown to seller; keep car AVAILABLE until sale is finalized
		if (car.getStatus() == CarStatus.RESERVED) {
			car.setStatus(CarStatus.AVAILABLE);
			carRepository.save(car);
		}

		Booking booking = Booking.builder()
				.bookingNumber(bookingNumber)
				.car(car)
				.buyer(buyer)
				.tokenAmount(tokenAmount)
				.status(BookingStatus.PENDING_PAYMENT)
				.paymentStatus(PaymentStatus.UNPAID)
				.expiresAt(expiresAt)
				.build();

		Booking savedBooking = bookingRepository.save(booking);
		log.info("Created booking {} for car {} by buyer {}", bookingNumber, carId, buyer.getEmail());

		return mapToBookingDTO(savedBooking);
	}

	@Override
	public PageResponse<BookingDTO> getBuyerBookings(Long buyerId, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		Page<Booking> bookingPage = bookingRepository.findByBuyerId(buyerId, pageable);
		List<BookingDTO> dtoList = bookingPage.getContent().stream()
				.map(this::mapToBookingDTO)
				.collect(Collectors.toList());

		return PageResponse.of(bookingPage, dtoList);
	}

	@Override
	public BookingDTO getBuyerBookingById(Long buyerId, Long bookingId) {
		Booking booking = bookingRepository.findDetailedById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

		if (!booking.getBuyer().getId().equals(buyerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this booking");
		}

		return mapToBookingDTO(booking);
	}

	@Override
	@Transactional
	public void cancelBooking(Long buyerId, Long bookingId) {
		Booking booking = bookingRepository.findDetailedById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

		if (!booking.getBuyer().getId().equals(buyerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this booking");
		}

		if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
			throw new InvalidInputException("Only pending bookings can be cancelled by the buyer");
		}

		booking.setStatus(BookingStatus.CANCELLED);
		Car car = booking.getCar();
		car.setStatus(CarStatus.AVAILABLE);

		carRepository.save(car);
		bookingRepository.save(booking);
		log.info("Buyer {} cancelled booking {}. Car {} released to AVAILABLE", buyerId, booking.getBookingNumber(), car.getId());

		try {
			paymentClient.closePaymentOrder(booking.getId(), internalApiKey);
		} catch (Exception e) {
			log.warn("Best-effort notification to payment-service to close order for booking {} failed: {}", bookingId, e.getMessage());
		}
	}

	@Override
	public PageResponse<BookingDTO> getSellerBookings(Long sellerId, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		Page<Booking> bookingPage = bookingRepository.findBySellerId(sellerId, pageable);
		List<BookingDTO> dtoList = bookingPage.getContent().stream()
				.map(this::mapToBookingDTO)
				.collect(Collectors.toList());

		return PageResponse.of(bookingPage, dtoList);
	}

	@Override
	@Transactional
	public void completeBooking(Long sellerId, Long bookingId) {
		Booking booking = bookingRepository.findDetailedById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

		Car car = booking.getCar();
		if (!car.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("Forbidden: You do not own the car associated with this booking");
		}

		if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
			throw new InvalidInputException("Only active bookings can be completed");
		}

		booking.setStatus(BookingStatus.COMPLETED);
		car.setStatus(CarStatus.SOLD);

		carRepository.save(car);
		bookingRepository.save(booking);
		log.info("Seller {} completed booking {}. Car {} marked SOLD", sellerId, booking.getBookingNumber(), car.getId());
	}

	@Override
	public PageResponse<BookingDTO> getAdminBookings(int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		Page<Booking> bookingPage = bookingRepository.findAllDetailed(pageable);
		List<BookingDTO> dtoList = bookingPage.getContent().stream()
				.map(this::mapToBookingDTO)
				.collect(Collectors.toList());

		return PageResponse.of(bookingPage, dtoList);
	}

	@Override
	public InternalBookingDTO getInternalBooking(Long bookingId) {
		Booking booking = bookingRepository.findDetailedById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

		User buyer = booking.getBuyer();
		return InternalBookingDTO.builder()
				.id(booking.getId())
				.bookingNumber(booking.getBookingNumber())
				.tokenAmount(booking.getTokenAmount())
				.status(booking.getStatus())
				.paymentStatus(booking.getPaymentStatus())
				.buyerId(buyer.getId())
				.buyerName(buyer.getCompleteName())
				.buyerEmail(buyer.getEmail())
				.buyerPhone(buyer.getPhone())
				.expiresAt(booking.getExpiresAt())
				.build();
	}

	@Override
	@Transactional
	public void updatePaymentStatus(Long bookingId, PaymentStatus newStatus, String razorpayPaymentId) {
		Booking booking = bookingRepository.findDetailedById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

		Car car = booking.getCar();
		booking.setPaymentStatus(newStatus);

		if (newStatus == PaymentStatus.PAID) {
			booking.setStatus(BookingStatus.CONFIRMED);
			// Car remains RESERVED
			log.info("Booking {} marked CONFIRMED via payment verification (payment id: {})", booking.getBookingNumber(), razorpayPaymentId);
		} else if (newStatus == PaymentStatus.FAILED) {
			// Keep booking PENDING_PAYMENT until expiry so buyer can retry
			log.info("Payment failed for booking {}. Retaining PENDING_PAYMENT status until expiry", booking.getBookingNumber());
		} else if (newStatus == PaymentStatus.REFUNDED) {
			booking.setStatus(BookingStatus.CANCELLED);
			car.setStatus(CarStatus.AVAILABLE);
			carRepository.save(car);
			log.info("Booking {} CANCELLED due to refund. Car {} returned to AVAILABLE", booking.getBookingNumber(), car.getId());
		}

		bookingRepository.save(booking);
	}

	@Override
	@Scheduled(fixedRate = 60000)
	@Transactional
	public void expirePendingBookings() {
		LocalDateTime now = LocalDateTime.now();
		List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now);

		for (Booking b : expiredBookings) {
			b.setStatus(BookingStatus.EXPIRED);
			Car car = b.getCar();
			if (car.getStatus() == CarStatus.RESERVED) {
				car.setStatus(CarStatus.AVAILABLE);
				carRepository.save(car);
			}
			bookingRepository.save(b);
			log.info("Booking {} expired. Car {} released to AVAILABLE", b.getBookingNumber(), car.getId());

			try {
				paymentClient.closePaymentOrder(b.getId(), internalApiKey);
			} catch (Exception e) {
				log.warn("Best-effort notification to payment-service to close order for expired booking {} failed: {}", b.getId(), e.getMessage());
			}
		}
	}

	private BookingDTO mapToBookingDTO(Booking booking) {
		Car car = booking.getCar();
		User buyer = booking.getBuyer();
		User seller = car.getSeller();

		String showroom = null;
		if (seller != null && seller.getSellerProfile() != null) {
			showroom = seller.getSellerProfile().getShowroomName();
		}

		return BookingDTO.builder()
				.id(booking.getId())
				.bookingNumber(booking.getBookingNumber())
				.carId(car.getId())
				.carTitle(car.getTitle())
				.carBrand(car.getBrand())
				.carModel(car.getModel())
				.carPrice(car.getPrice())
				.primaryImageUrl(car.getPrimaryImageUrl())
				.buyerId(buyer.getId())
				.buyerName(buyer.getCompleteName())
				.buyerEmail(buyer.getEmail())
				.buyerPhone(buyer.getPhone())
				.sellerId(seller != null ? seller.getId() : null)
				.sellerName(seller != null ? seller.getCompleteName() : null)
				.sellerShowroom(showroom)
				.tokenAmount(booking.getTokenAmount())
				.status(booking.getStatus())
				.paymentStatus(booking.getPaymentStatus())
				.expiresAt(booking.getExpiresAt())
				.createdOn(booking.getCreatedOn())
				.version(booking.getVersion())
				.build();
	}
}
