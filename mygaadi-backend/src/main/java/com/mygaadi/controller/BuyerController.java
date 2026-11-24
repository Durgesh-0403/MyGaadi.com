package com.mygaadi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.dtos.ApiResponse;
import com.mygaadi.dtos.BookingCreateDTO;
import com.mygaadi.dtos.BookingDTO;
import com.mygaadi.dtos.CarDTO;
import com.mygaadi.dtos.JwtDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.service.BookingService;
import com.mygaadi.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/buyer")
@RequiredArgsConstructor
@Tag(name = "Buyer Operations", description = "Endpoints restricted to buyers")
public class BuyerController {

	private final WishlistService wishlistService;
	private final BookingService bookingService;

	@GetMapping("/wishlist")
	@Operation(summary = "Get current buyer's active wishlist")
	public ResponseEntity<List<CarDTO>> getWishlist(@AuthenticationPrincipal JwtDTO jwtDTO) {
		return ResponseEntity.ok(wishlistService.getBuyerWishlist(jwtDTO.getUserId()));
	}

	@PostMapping("/wishlist/{carId}")
	@Operation(summary = "Add or restore car in buyer's wishlist")
	public ResponseEntity<ApiResponse> addToWishlist(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long carId) {
		wishlistService.addToWishlist(jwtDTO.getUserId(), carId);
		return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("SUCCESS", "Car added to wishlist"));
	}

	@DeleteMapping("/wishlist/{carId}")
	@Operation(summary = "Remove car from wishlist (soft delete)")
	public ResponseEntity<ApiResponse> removeFromWishlist(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long carId) {
		wishlistService.removeFromWishlist(jwtDTO.getUserId(), carId);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Car removed from wishlist"));
	}

	@PostMapping("/bookings")
	@Operation(summary = "Create token booking for a car")
	public ResponseEntity<BookingDTO> createBooking(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@Valid @RequestBody BookingCreateDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(jwtDTO.getUserId(), dto.getCarId()));
	}

	@GetMapping("/bookings")
	@Operation(summary = "Get paginated bookings of the current buyer")
	public ResponseEntity<PageResponse<BookingDTO>> getMyBookings(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(bookingService.getBuyerBookings(jwtDTO.getUserId(), page, size));
	}

	@GetMapping("/bookings/{id}")
	@Operation(summary = "Get specific booking detail by ID")
	public ResponseEntity<BookingDTO> getBookingById(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id) {
		return ResponseEntity.ok(bookingService.getBuyerBookingById(jwtDTO.getUserId(), id));
	}

	@PatchMapping("/bookings/{id}/cancel")
	@Operation(summary = "Cancel a pending booking")
	public ResponseEntity<ApiResponse> cancelBooking(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id) {
		bookingService.cancelBooking(jwtDTO.getUserId(), id);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Booking cancelled successfully"));
	}
}
