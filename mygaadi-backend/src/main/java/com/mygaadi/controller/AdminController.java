package com.mygaadi.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.dtos.AdminDashboardDTO;
import com.mygaadi.dtos.ApiResponse;
import com.mygaadi.dtos.BookingDTO;
import com.mygaadi.dtos.CarDTO;
import com.mygaadi.dtos.JwtDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.dtos.SellerKycStatusUpdateDTO;
import com.mygaadi.dtos.SellerProfileDTO;
import com.mygaadi.dtos.UserDTO;
import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.UserRole;
import com.mygaadi.service.AdminService;
import com.mygaadi.service.BookingService;
import com.mygaadi.service.CarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Administrator Operations", description = "Endpoints restricted to administrators")
public class AdminController {

	private final AdminService adminService;
	private final CarService carService;
	private final BookingService bookingService;

	@GetMapping({"/dashboard", "/dashboard/stats"})
	@Operation(summary = "Get platform moderation dashboard statistics")
	public ResponseEntity<AdminDashboardDTO> getDashboard() {
		return ResponseEntity.ok(adminService.getDashboardMetrics());
	}

	@GetMapping("/users")
	@Operation(summary = "List and search all users with role filter")
	public ResponseEntity<PageResponse<UserDTO>> getUsers(
			@RequestParam(required = false) UserRole role,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "15") int size) {
		return ResponseEntity.ok(adminService.getUsers(role, q, page, size));
	}

	@PatchMapping("/users/{id}/block")
	@Operation(summary = "Block a user account")
	public ResponseEntity<ApiResponse> blockUser(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id) {
		adminService.blockUser(id, jwtDTO.getUserId());
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "User account blocked successfully"));
	}

	@PatchMapping("/users/{id}/unblock")
	@Operation(summary = "Unblock a user account")
	public ResponseEntity<ApiResponse> unblockUser(@PathVariable Long id) {
		adminService.unblockUser(id);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "User account unblocked successfully"));
	}

	@DeleteMapping("/users/{id}")
	@Operation(summary = "Delete buyer or seller user account")
	public ResponseEntity<ApiResponse> deleteUser(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id) {
		adminService.deleteUser(id, jwtDTO.getUserId());
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "User account deleted successfully"));
	}

	@PatchMapping("/sellers/{id}/verify")
	@Operation(summary = "Verify a seller's showroom")
	public ResponseEntity<ApiResponse> verifySeller(@PathVariable Long id) {
		adminService.verifySeller(id);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Seller showroom verified"));
	}

	@PatchMapping("/sellers/{id}/kyc")
	@Operation(summary = "Approve or reject seller KYC verification")
	public ResponseEntity<SellerProfileDTO> updateSellerKyc(
			@PathVariable Long id,
			@RequestBody SellerKycStatusUpdateDTO dto) {
		return ResponseEntity.ok(adminService.updateSellerKycStatus(id, dto.getStatus(), dto.getRejectionReason()));
	}

	@GetMapping("/sellers/{id}/kyc")
	@Operation(summary = "Get seller KYC verification details")
	public ResponseEntity<SellerProfileDTO> getSellerKyc(@PathVariable Long id) {
		return ResponseEntity.ok(adminService.getSellerKyc(id));
	}

	@GetMapping("/cars")
	@Operation(summary = "List cars for admin moderation (with deleted & approval filter)")
	public ResponseEntity<PageResponse<CarDTO>> getCars(
			@RequestParam(required = false) ApprovalStatus approvalStatus,
			@RequestParam(required = false) Boolean deleted,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "15") int size) {
		return ResponseEntity.ok(carService.getAdminCars(approvalStatus, deleted, q, page, size));
	}

	@PatchMapping("/cars/{id}/approve")
	@Operation(summary = "Approve car listing")
	public ResponseEntity<CarDTO> approveCar(@PathVariable Long id) {
		return ResponseEntity.ok(carService.approveCar(id));
	}

	@PatchMapping("/cars/{id}/reject")
	@Operation(summary = "Reject car listing with reason")
	public ResponseEntity<CarDTO> rejectCar(
			@PathVariable Long id,
			@RequestBody(required = false) Map<String, String> body) {
		String reason = body != null ? body.get("reason") : "Does not meet marketplace standards";
		return ResponseEntity.ok(carService.rejectCar(id, reason));
	}

	@DeleteMapping("/cars/{id}")
	@Operation(summary = "Admin soft-delete car listing")
	public ResponseEntity<ApiResponse> deleteCar(@PathVariable Long id) {
		carService.adminDeleteCar(id);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Car deleted successfully"));
	}

	@PatchMapping("/cars/{id}/restore")
	@Operation(summary = "Restore soft-deleted car listing")
	public ResponseEntity<CarDTO> restoreCar(@PathVariable Long id) {
		return ResponseEntity.ok(carService.restoreCar(id));
	}

	@GetMapping("/bookings")
	@Operation(summary = "List all platform bookings")
	public ResponseEntity<PageResponse<BookingDTO>> getBookings(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "15") int size) {
		return ResponseEntity.ok(bookingService.getAdminBookings(page, size));
	}
}
