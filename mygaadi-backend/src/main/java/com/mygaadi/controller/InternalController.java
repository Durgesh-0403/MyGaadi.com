package com.mygaadi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.dtos.ApiResponse;
import com.mygaadi.dtos.InternalBookingDTO;
import com.mygaadi.dtos.InternalCarExportDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.dtos.PaymentStatusUpdateDTO;
import com.mygaadi.service.BookingService;
import com.mygaadi.service.CarService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Hidden
public class InternalController {

	private final BookingService bookingService;
	private final CarService carService;

	@GetMapping("/bookings/{id}")
	public ResponseEntity<InternalBookingDTO> getInternalBooking(@PathVariable Long id) {
		return ResponseEntity.ok(bookingService.getInternalBooking(id));
	}

	@PutMapping("/bookings/{id}/payment-status")
	public ResponseEntity<ApiResponse> updatePaymentStatus(
			@PathVariable Long id,
			@Valid @RequestBody PaymentStatusUpdateDTO dto) {
		bookingService.updatePaymentStatus(id, dto.getPaymentStatus(), dto.getRazorpayPaymentId());
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Payment status updated successfully"));
	}

	@GetMapping("/cars/export")
	public ResponseEntity<PageResponse<InternalCarExportDTO>> exportCarsForRag(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return ResponseEntity.ok(carService.exportCarsForRag(page, size));
	}
}
