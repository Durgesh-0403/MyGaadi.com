package com.mygaadi.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.payment.dtos.AdminPaymentResponseDTO;
import com.mygaadi.payment.dtos.ApiResponse;
import com.mygaadi.payment.dtos.JwtDTO;
import com.mygaadi.payment.dtos.OrderRequestDTO;
import com.mygaadi.payment.dtos.OrderResponseDTO;
import com.mygaadi.payment.dtos.PageResponse;
import com.mygaadi.payment.dtos.PaymentDTO;
import com.mygaadi.payment.dtos.PaymentFailureDTO;
import com.mygaadi.payment.dtos.PaymentVerifyDTO;
import com.mygaadi.payment.dtos.RefundRequestDTO;
import com.mygaadi.payment.entities.PaymentRecordStatus;
import com.mygaadi.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay token booking payment endpoints")
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/orders")
	@Operation(summary = "Create Razorpay payment order for booking token")
	public ResponseEntity<OrderResponseDTO> createOrder(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@Valid @RequestBody OrderRequestDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(paymentService.createOrder(jwtDTO.getUserId(), dto.getBookingId()));
	}

	@PostMapping("/verify")
	@Operation(summary = "Verify Razorpay checkout HMAC-SHA256 signature")
	public ResponseEntity<ApiResponse> verifyPayment(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@Valid @RequestBody PaymentVerifyDTO dto) {
		return ResponseEntity.ok(paymentService.verifyPayment(jwtDTO.getUserId(), dto));
	}

	@PostMapping("/failure")
	@Operation(summary = "Record customer payment failure / cancellation")
	public ResponseEntity<ApiResponse> recordFailure(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@Valid @RequestBody PaymentFailureDTO dto) {
		return ResponseEntity.ok(paymentService.recordFailure(jwtDTO.getUserId(), dto));
	}

	@PostMapping("/webhook")
	@Operation(summary = "Razorpay server-to-server webhook endpoint")
	public ResponseEntity<ApiResponse> handleWebhook(
			@RequestBody String rawPayload,
			@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
			@RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
		paymentService.handleWebhook(rawPayload, signature, eventId);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Webhook processed successfully"));
	}

	@GetMapping("/booking/{bookingId}")
	@Operation(summary = "Get payment record for a given booking")
	public ResponseEntity<PaymentDTO> getPaymentByBooking(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long bookingId) {
		return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId, jwtDTO.getUserId(), jwtDTO.getUserRole()));
	}

	@GetMapping("/my")
	@Operation(summary = "Get current buyer's payments")
	public ResponseEntity<PageResponse<PaymentDTO>> getMyPayments(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(paymentService.getMyPayments(jwtDTO.getUserId(), page, size));
	}

	@GetMapping("/admin")
	@Operation(summary = "List all platform payments (Admin only)")
	public ResponseEntity<AdminPaymentResponseDTO> getAdminPayments(
			@RequestParam(required = false) PaymentRecordStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "15") int size) {
		return ResponseEntity.ok(paymentService.getAdminPayments(status, page, size));
	}

	@PostMapping("/admin/{paymentId}/refund")
	@Operation(summary = "Trigger Razorpay refund for a paid transaction (Admin only)")
	public ResponseEntity<ApiResponse> initiateRefund(
			@PathVariable Long paymentId,
			@Valid @RequestBody RefundRequestDTO dto) {
		return ResponseEntity.ok(paymentService.initiateRefund(paymentId, dto.getReason()));
	}
}
