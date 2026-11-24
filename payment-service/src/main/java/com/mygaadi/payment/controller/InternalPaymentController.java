package com.mygaadi.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.payment.dtos.ApiResponse;
import com.mygaadi.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
@Hidden
public class InternalPaymentController {

	private final PaymentService paymentService;

	@PostMapping("/booking/{bookingId}/close")
	public ResponseEntity<ApiResponse> closePaymentForBooking(@PathVariable Long bookingId) {
		return ResponseEntity.ok(paymentService.closePaymentForBooking(bookingId));
	}
}
