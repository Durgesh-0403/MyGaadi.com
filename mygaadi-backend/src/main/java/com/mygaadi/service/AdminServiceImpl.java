package com.mygaadi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.custom_exceptions.ApiException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.AdminDashboardDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.dtos.SellerProfileDTO;
import com.mygaadi.dtos.UserDTO;
import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.BookingStatus;
import com.mygaadi.entities.Car;
import com.mygaadi.entities.CarStatus;
import com.mygaadi.entities.PaymentStatus;
import com.mygaadi.entities.SellerProfile;
import com.mygaadi.entities.User;
import com.mygaadi.entities.UserRole;
import com.mygaadi.repository.BookingRepository;
import com.mygaadi.repository.CarRepository;
import com.mygaadi.repository.SellerProfileRepository;
import com.mygaadi.repository.UserRepository;
import com.mygaadi.repository.WishlistItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

	private final UserRepository userRepository;
	private final SellerProfileRepository sellerProfileRepository;
	private final CarRepository carRepository;
	private final BookingRepository bookingRepository;
	private final WishlistItemRepository wishlistItemRepository;
	private final ModelMapper modelMapper;

	@Override
	public AdminDashboardDTO getDashboardMetrics() {
		Map<String, Long> usersByRole = new HashMap<>();
		for (UserRole role : UserRole.values()) {
			usersByRole.put(role.name(), userRepository.countByUserRole(role));
		}

		Map<String, Long> carsByApproval = new HashMap<>();
		for (ApprovalStatus status : ApprovalStatus.values()) {
			carsByApproval.put(status.name(), carRepository.countByApprovalStatusAndDeletedFalse(status));
		}

		Map<String, Long> carsByStatus = new HashMap<>();
		for (CarStatus status : CarStatus.values()) {
			carsByStatus.put(status.name(), carRepository.countByStatusAndDeletedFalse(status));
		}

		Map<String, Long> bookingsByStatus = new HashMap<>();
		for (BookingStatus status : BookingStatus.values()) {
			bookingsByStatus.put(status.name(), bookingRepository.countByStatus(status));
		}

		long totalUsers = userRepository.count();
		long totalCars = carRepository.count();
		long totalBookings = bookingRepository.count();
		BigDecimal totalRevenue = bookingRepository.sumPaidTokenAmount(PaymentStatus.PAID);

		return AdminDashboardDTO.builder()
				.usersByRole(usersByRole)
				.carsByApprovalStatus(carsByApproval)
				.carsByStatus(carsByStatus)
				.bookingsByStatus(bookingsByStatus)
				.totalUsers(totalUsers)
				.totalCars(totalCars)
				.totalBookings(totalBookings)
				.totalTokenRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
				.build();
	}

	@Override
	public PageResponse<UserDTO> getUsers(UserRole role, String q, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		String search = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
		Page<User> userPage = userRepository.searchUsers(role, search, pageable);

		List<UserDTO> dtoList = userPage.getContent().stream()
				.map(user -> {
					UserDTO dto = modelMapper.map(user, UserDTO.class);
					dto.setCompleteName(user.getCompleteName());
					if (user.getSellerProfile() != null) {
						dto.setSellerProfile(modelMapper.map(user.getSellerProfile(), SellerProfileDTO.class));
					}
					return dto;
				})
				.collect(Collectors.toList());

		return PageResponse.of(userPage, dtoList);
	}

	@Override
	@Transactional
	public void blockUser(Long userId, Long currentAdminId) {
		User targetUser = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		// Section 5: An admin cannot block an admin
		if (targetUser.getUserRole() == UserRole.ROLE_ADMIN) {
			throw new ApiException("Security policy violation: An administrator cannot block another administrator");
		}

		targetUser.setActive(false);
		userRepository.save(targetUser);
		log.info("Admin {} blocked user account {}", currentAdminId, targetUser.getEmail());
	}

	@Override
	@Transactional
	public void unblockUser(Long userId) {
		User targetUser = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		targetUser.setActive(true);
		userRepository.save(targetUser);
		log.info("User account {} unblocked", targetUser.getEmail());
	}

	@Override
	@Transactional
	public void deleteUser(Long userId, Long currentAdminId) {
		User targetUser = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		if (targetUser.getUserRole() == UserRole.ROLE_ADMIN) {
			throw new ApiException("Security policy violation: An administrator cannot be deleted");
		}

		if (targetUser.getId().equals(currentAdminId)) {
			throw new ApiException("Cannot delete your own administrator account");
		}

		// Cleanup wishlist records
		wishlistItemRepository.deleteByUserId(userId);

		// Cleanup bookings where buyer is targetUser
		bookingRepository.deleteByBuyerId(userId);

		// If seller: cleanup cars, car bookings and images
		List<Car> sellerCars = carRepository.findBySellerId(userId);
		for (Car car : sellerCars) {
			bookingRepository.deleteByCarId(car.getId());
			carRepository.delete(car);
		}

		userRepository.delete(targetUser);
		log.info("Admin {} deleted user account {} (ID: {})", currentAdminId, targetUser.getEmail(), userId);
	}

	@Override
	@Transactional
	public void verifySeller(Long sellerId) {
		updateSellerKycStatus(sellerId, "VERIFIED", null);
	}

	@Override
	@Transactional
	public SellerProfileDTO updateSellerKycStatus(Long sellerId, String status, String rejectionReason) {
		SellerProfile profile = sellerProfileRepository.findById(sellerId)
				.orElseThrow(() -> new ResourceNotFoundException("Seller profile not found with id: " + sellerId));

		if ("VERIFIED".equalsIgnoreCase(status)) {
			profile.setVerified(true);
			profile.setKycStatus("VERIFIED");
			profile.setKycVerifiedAt(LocalDateTime.now());
			profile.setKycRejectionReason(null);
			log.info("Admin verified KYC for seller {}: {}", sellerId, profile.getShowroomName());
		} else if ("REJECTED".equalsIgnoreCase(status)) {
			profile.setVerified(false);
			profile.setKycStatus("REJECTED");
			profile.setKycRejectionReason(rejectionReason != null && !rejectionReason.trim().isEmpty() ? rejectionReason.trim() : "Uploaded document could not be verified");
			log.info("Admin rejected KYC for seller {}: {}", sellerId, profile.getKycRejectionReason());
		} else {
			throw new ApiException("Invalid KYC status. Must be VERIFIED or REJECTED");
		}

		SellerProfile saved = sellerProfileRepository.save(profile);
		return modelMapper.map(saved, SellerProfileDTO.class);
	}

	@Override
	public SellerProfileDTO getSellerKyc(Long sellerId) {
		SellerProfile profile = sellerProfileRepository.findById(sellerId)
				.orElseThrow(() -> new ResourceNotFoundException("Seller profile not found with id: " + sellerId));
		return modelMapper.map(profile, SellerProfileDTO.class);
	}
}
