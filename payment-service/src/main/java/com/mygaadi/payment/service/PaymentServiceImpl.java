package com.mygaadi.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.payment.client.BackendClient;
import com.mygaadi.payment.custom_exceptions.InvalidInputException;
import com.mygaadi.payment.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.payment.dtos.AdminPaymentResponseDTO;
import com.mygaadi.payment.dtos.ApiResponse;
import com.mygaadi.payment.dtos.InternalBookingDTO;
import com.mygaadi.payment.dtos.OrderResponseDTO;
import com.mygaadi.payment.dtos.PageResponse;
import com.mygaadi.payment.dtos.PaymentDTO;
import com.mygaadi.payment.dtos.PaymentFailureDTO;
import com.mygaadi.payment.dtos.PaymentPrefillDTO;
import com.mygaadi.payment.dtos.PaymentStatusUpdateDTO;
import com.mygaadi.payment.dtos.PaymentVerifyDTO;
import com.mygaadi.payment.entities.Payment;
import com.mygaadi.payment.entities.PaymentRecordStatus;
import com.mygaadi.payment.entities.WebhookEvent;
import com.mygaadi.payment.repository.PaymentRepository;
import com.mygaadi.payment.repository.WebhookEventRepository;
import com.mygaadi.payment.razorpay.RazorpayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final WebhookEventRepository webhookEventRepository;
	private final RazorpayService razorpayService;
	private final BackendClient backendClient;

	@Override
	@Transactional
	public OrderResponseDTO createOrder(Long buyerId, Long bookingId) {
		InternalBookingDTO booking = backendClient.getBookingById(bookingId);

		if (booking == null) {
			throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
		}

		if (!booking.getBuyerId().equals(buyerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this booking");
		}

		if (!"PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())) {
			throw new InvalidInputException("Booking is not in PENDING_PAYMENT status (current: " + booking.getStatus() + ")");
		}

		if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new InvalidInputException("Booking has expired. Please create a new booking.");
		}

		// Always take amount from backend booking record
		BigDecimal amount = booking.getTokenAmount();
		String bookingNumber = booking.getBookingNumber();

		// Check for an existing unexpired CREATED payment record to reuse
		Optional<Payment> existingPayment = paymentRepository.findFirstByBookingIdAndStatusOrderByCreatedOnDesc(
				bookingId,
				PaymentRecordStatus.CREATED
		);

		Payment payment;
		if (existingPayment.isPresent()) {
			payment = existingPayment.get();
			log.info("Reusing existing CREATED payment order {} for booking {}", payment.getRazorpayOrderId(), bookingNumber);
		} else {
			String razorpayOrderId = razorpayService.createOrder(amount, bookingNumber);

			payment = Payment.builder()
					.bookingId(bookingId)
					.bookingNumber(bookingNumber)
					.buyerId(buyerId)
					.amount(amount)
					.currency("INR")
					.razorpayOrderId(razorpayOrderId)
					.status(PaymentRecordStatus.CREATED)
					.backendNotified(false)
					.build();

			payment = paymentRepository.save(payment);
			log.info("Created new payment record {} for booking {}", payment.getId(), bookingNumber);
		}

		PaymentPrefillDTO prefill = PaymentPrefillDTO.builder()
				.name(booking.getBuyerName())
				.email(booking.getBuyerEmail())
				.contact(booking.getBuyerPhone())
				.build();

		return OrderResponseDTO.builder()
				.paymentId(payment.getId())
				.razorpayOrderId(payment.getRazorpayOrderId())
				.razorpayKeyId(razorpayService.getKeyId())
				.amount(amount)
				.currency("INR")
				.bookingNumber(bookingNumber)
				.prefill(prefill)
				.build();
	}

	@Override
	@Transactional
	public ApiResponse verifyPayment(Long buyerId, PaymentVerifyDTO dto) {
		Payment payment = paymentRepository.findByRazorpayOrderId(dto.getOrderId())
				.orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order: " + dto.getOrderId()));

		if (!payment.getBuyerId().equals(buyerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this payment record");
		}

		// Idempotency check: repeating verify on a PAID payment returns success
		if (payment.getStatus() == PaymentRecordStatus.PAID) {
			log.info("Payment for order {} is already marked PAID. Returning idempotent success.", dto.getOrderId());
			return new ApiResponse("SUCCESS", "Payment already verified successfully");
		}

		boolean isValid = razorpayService.verifyPaymentSignature(dto.getOrderId(), dto.getPaymentId(), dto.getSignature());
		if (!isValid) {
			log.warn("Invalid payment signature detected for order {} and payment {}", dto.getOrderId(), dto.getPaymentId());
			throw new InvalidInputException("Payment signature verification failed");
		}

		payment.setRazorpayPaymentId(dto.getPaymentId());
		payment.setRazorpaySignature(dto.getSignature());
		payment.setStatus(PaymentRecordStatus.PAID);

		// Synchronously update backend booking status
		try {
			backendClient.updatePaymentStatus(
					payment.getBookingId(),
					new PaymentStatusUpdateDTO("PAID", dto.getPaymentId())
			);
			payment.setBackendNotified(true);
			log.info("Successfully verified payment and updated backend for booking {}", payment.getBookingId());
		} catch (Exception e) {
			log.error("Failed to notify core backend for booking {} after payment verification: {}", payment.getBookingId(), e.getMessage());
			payment.setBackendNotified(false);
		}

		paymentRepository.save(payment);
		return new ApiResponse("SUCCESS", "Payment verified and booking confirmed successfully");
	}

	@Override
	@Transactional
	public ApiResponse recordFailure(Long buyerId, PaymentFailureDTO dto) {
		Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(dto.getOrderId());
		if (paymentOpt.isPresent()) {
			Payment payment = paymentOpt.get();
			if (payment.getStatus() == PaymentRecordStatus.CREATED) {
				payment.setStatus(PaymentRecordStatus.FAILED);
				payment.setFailureReason(dto.getReason() != null ? dto.getReason() : "Payment failed / cancelled by buyer");

				try {
					backendClient.updatePaymentStatus(
							payment.getBookingId(),
							new PaymentStatusUpdateDTO("FAILED", null)
					);
					payment.setBackendNotified(true);
				} catch (Exception e) {
					log.warn("Failed to notify backend of payment failure for booking {}: {}", payment.getBookingId(), e.getMessage());
					payment.setBackendNotified(false);
				}

				paymentRepository.save(payment);
			}
		}
		return new ApiResponse("SUCCESS", "Payment failure noted");
	}

	@Override
	@Transactional
	public void handleWebhook(String rawBody, String signature, String eventId) {
		// Webhook Deduplication
		if (eventId != null && !eventId.isBlank()) {
			if (webhookEventRepository.existsByEventId(eventId)) {
				log.info("Duplicate webhook event {} ignored.", eventId);
				return;
			}
		}

		// Webhook Signature Verification
		boolean isValid = razorpayService.verifyWebhookSignature(rawBody, signature);
		if (!isValid) {
			log.warn("Rejected Razorpay webhook with invalid signature.");
			throw new InvalidInputException("Webhook signature verification failed");
		}

		JSONObject json = new JSONObject(rawBody);
		String eventType = json.optString("event");
		log.info("Processing verified Razorpay webhook event: {}", eventType);

		JSONObject payload = json.optJSONObject("payload");
		if (payload != null) {
			if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
				handlePaymentCapturedWebhook(payload);
			} else if ("payment.failed".equals(eventType)) {
				handlePaymentFailedWebhook(payload);
			} else if ("refund.processed".equals(eventType)) {
				handleRefundProcessedWebhook(payload);
			}
		}

		if (eventId != null && !eventId.isBlank()) {
			WebhookEvent webhookEvent = WebhookEvent.builder()
					.eventId(eventId)
					.eventType(eventType != null ? eventType : "unknown")
					.processedAt(LocalDateTime.now())
					.build();
			webhookEventRepository.save(webhookEvent);
		}
	}

	private void handlePaymentCapturedWebhook(JSONObject payload) {
		JSONObject paymentObj = payload.optJSONObject("payment");
		JSONObject entity = paymentObj != null ? paymentObj.optJSONObject("entity") : null;

		if (entity != null) {
			String orderId = entity.optString("order_id");
			String paymentId = entity.optString("id");
			String method = entity.optString("method");

			Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(orderId);
			if (paymentOpt.isPresent()) {
				Payment payment = paymentOpt.get();
				if (payment.getStatus() != PaymentRecordStatus.PAID) {
					payment.setStatus(PaymentRecordStatus.PAID);
					payment.setRazorpayPaymentId(paymentId);
					payment.setMethod(method);

					try {
						backendClient.updatePaymentStatus(
								payment.getBookingId(),
								new PaymentStatusUpdateDTO("PAID", paymentId)
						);
						payment.setBackendNotified(true);
					} catch (Exception e) {
						log.warn("Webhook: failed to notify backend for booking {}: {}", payment.getBookingId(), e.getMessage());
						payment.setBackendNotified(false);
					}

					paymentRepository.save(payment);
					log.info("Webhook marked payment {} PAID for order {}", paymentId, orderId);
				}
			}
		}
	}

	private void handlePaymentFailedWebhook(JSONObject payload) {
		JSONObject paymentObj = payload.optJSONObject("payment");
		JSONObject entity = paymentObj != null ? paymentObj.optJSONObject("entity") : null;

		if (entity != null) {
			String orderId = entity.optString("order_id");
			String errorDesc = entity.optString("error_description");

			Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(orderId);
			if (paymentOpt.isPresent()) {
				Payment payment = paymentOpt.get();
				if (payment.getStatus() == PaymentRecordStatus.CREATED) {
					payment.setStatus(PaymentRecordStatus.FAILED);
					payment.setFailureReason(errorDesc);

					try {
						backendClient.updatePaymentStatus(
								payment.getBookingId(),
								new PaymentStatusUpdateDTO("FAILED", null)
						);
						payment.setBackendNotified(true);
					} catch (Exception e) {
						payment.setBackendNotified(false);
					}

					paymentRepository.save(payment);
				}
			}
		}
	}

	private void handleRefundProcessedWebhook(JSONObject payload) {
		JSONObject refundObj = payload.optJSONObject("refund");
		JSONObject entity = refundObj != null ? refundObj.optJSONObject("entity") : null;

		if (entity != null) {
			String paymentId = entity.optString("payment_id");
			String refundId = entity.optString("id");

			Optional<Payment> paymentOpt = paymentRepository.findByRazorpayPaymentId(paymentId);
			if (paymentOpt.isPresent()) {
				Payment payment = paymentOpt.get();
				payment.setStatus(PaymentRecordStatus.REFUNDED);
				payment.setRazorpayRefundId(refundId);

				try {
					backendClient.updatePaymentStatus(
							payment.getBookingId(),
							new PaymentStatusUpdateDTO("REFUNDED", paymentId)
					);
					payment.setBackendNotified(true);
				} catch (Exception e) {
					payment.setBackendNotified(false);
				}

				paymentRepository.save(payment);
				log.info("Webhook marked payment {} REFUNDED (refund id: {})", paymentId, refundId);
			}
		}
	}

	@Override
	public PaymentDTO getPaymentByBooking(Long bookingId, Long userId, String userRole) {
		Payment payment = paymentRepository.findFirstByBookingIdOrderByCreatedOnDesc(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("No payment record found for booking " + bookingId));

		if (!"ROLE_ADMIN".equals(userRole) && !payment.getBuyerId().equals(userId)) {
			throw new AccessDeniedException("Forbidden: You do not own this payment record");
		}

		return mapToPaymentDTO(payment);
	}

	@Override
	public PageResponse<PaymentDTO> getMyPayments(Long buyerId, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		Page<Payment> paymentPage = paymentRepository.findByBuyerId(buyerId, pageable);
		List<PaymentDTO> dtoList = paymentPage.getContent().stream()
				.map(this::mapToPaymentDTO)
				.collect(Collectors.toList());

		return PageResponse.of(paymentPage, dtoList);
	}

	@Override
	public AdminPaymentResponseDTO getAdminPayments(PaymentRecordStatus status, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		Page<Payment> paymentPage = (status != null)
				? paymentRepository.findByStatus(status, pageable)
				: paymentRepository.findAll(pageable);

		List<PaymentDTO> dtoList = paymentPage.getContent().stream()
				.map(this::mapToPaymentDTO)
				.collect(Collectors.toList());

		BigDecimal totalPaid = paymentRepository.sumAmountByStatus(PaymentRecordStatus.PAID);
		BigDecimal totalRefunded = paymentRepository.sumAmountByStatus(PaymentRecordStatus.REFUNDED);

		return AdminPaymentResponseDTO.builder()
				.payments(PageResponse.of(paymentPage, dtoList))
				.totalPaidAmount(totalPaid != null ? totalPaid : BigDecimal.ZERO)
				.totalRefundedAmount(totalRefunded != null ? totalRefunded : BigDecimal.ZERO)
				.totalCount(paymentRepository.count())
				.build();
	}

	@Override
	@Transactional
	public ApiResponse initiateRefund(Long paymentId, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment record not found with id: " + paymentId));

		if (payment.getStatus() != PaymentRecordStatus.PAID) {
			throw new InvalidInputException("Only PAID transactions can be refunded (current status: " + payment.getStatus() + ")");
		}

		if (payment.getRazorpayPaymentId() == null) {
			throw new InvalidInputException("Cannot refund payment: Razorpay payment ID is missing");
		}

		String refundId = razorpayService.initiateRefund(payment.getRazorpayPaymentId(), payment.getAmount(), reason);
		payment.setStatus(PaymentRecordStatus.REFUND_INITIATED);
		payment.setRazorpayRefundId(refundId);
		paymentRepository.save(payment);

		log.info("Admin initiated refund {} for payment id {}", refundId, paymentId);
		return new ApiResponse("SUCCESS", "Refund initiated successfully with ID: " + refundId);
	}

	@Override
	@Transactional
	public ApiResponse closePaymentForBooking(Long bookingId) {
		Optional<Payment> paymentOpt = paymentRepository.findFirstByBookingIdAndStatusOrderByCreatedOnDesc(
				bookingId,
				PaymentRecordStatus.CREATED
		);

		if (paymentOpt.isPresent()) {
			Payment payment = paymentOpt.get();
			payment.setStatus(PaymentRecordStatus.FAILED);
			payment.setFailureReason("Booking expired or cancelled");
			paymentRepository.save(payment);
			log.info("Closed payment order {} for expired booking {}", payment.getRazorpayOrderId(), bookingId);
		}
		return new ApiResponse("SUCCESS", "Payment order closed");
	}

	@Override
	@Scheduled(fixedRate = 30000, initialDelayString = "${payment.retry.initial-delay:30000}")
	@Transactional
	public void retryBackendNotifications() {
		List<Payment> unnotified = paymentRepository.findByBackendNotifiedFalseAndStatusIn(
				List.of(PaymentRecordStatus.PAID, PaymentRecordStatus.FAILED, PaymentRecordStatus.REFUNDED)
		);

		for (Payment p : unnotified) {
			try {
				backendClient.updatePaymentStatus(
						p.getBookingId(),
						new PaymentStatusUpdateDTO(p.getStatus().name(), p.getRazorpayPaymentId())
				);
				p.setBackendNotified(true);
				paymentRepository.save(p);
				log.info("Retry job successfully synced payment status for booking {}", p.getBookingId());
			} catch (Exception e) {
				log.warn("Retry job: failed to sync payment status for booking {}: {}", p.getBookingId(), e.getMessage());
			}
		}
	}

	private PaymentDTO mapToPaymentDTO(Payment p) {
		return PaymentDTO.builder()
				.id(p.getId())
				.bookingId(p.getBookingId())
				.bookingNumber(p.getBookingNumber())
				.buyerId(p.getBuyerId())
				.amount(p.getAmount())
				.currency(p.getCurrency())
				.razorpayOrderId(p.getRazorpayOrderId())
				.razorpayPaymentId(p.getRazorpayPaymentId())
				.status(p.getStatus())
				.failureReason(p.getFailureReason())
				.method(p.getMethod())
				.razorpayRefundId(p.getRazorpayRefundId())
				.backendNotified(p.isBackendNotified())
				.createdOn(p.getCreatedOn())
				.lastUpdated(p.getLastUpdated())
				.build();
	}
}
