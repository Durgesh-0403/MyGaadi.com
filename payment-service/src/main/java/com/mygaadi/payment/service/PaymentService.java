package com.mygaadi.payment.service;

import com.mygaadi.payment.dtos.AdminPaymentResponseDTO;
import com.mygaadi.payment.dtos.ApiResponse;
import com.mygaadi.payment.dtos.OrderResponseDTO;
import com.mygaadi.payment.dtos.PageResponse;
import com.mygaadi.payment.dtos.PaymentDTO;
import com.mygaadi.payment.dtos.PaymentFailureDTO;
import com.mygaadi.payment.dtos.PaymentVerifyDTO;
import com.mygaadi.payment.entities.PaymentRecordStatus;

public interface PaymentService {

	OrderResponseDTO createOrder(Long buyerId, Long bookingId);

	ApiResponse verifyPayment(Long buyerId, PaymentVerifyDTO dto);

	ApiResponse recordFailure(Long buyerId, PaymentFailureDTO dto);

	void handleWebhook(String rawBody, String signature, String eventId);

	PaymentDTO getPaymentByBooking(Long bookingId, Long userId, String userRole);

	PageResponse<PaymentDTO> getMyPayments(Long buyerId, int page, int size);

	AdminPaymentResponseDTO getAdminPayments(PaymentRecordStatus status, int page, int size);

	ApiResponse initiateRefund(Long paymentId, String reason);

	ApiResponse closePaymentForBooking(Long bookingId);

	void retryBackendNotifications();
}
