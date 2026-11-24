package com.mygaadi.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.mygaadi.dtos.ApiResponse;

@FeignClient(name = "payment-service", url = "${payment.service.url:http://localhost:8082}")
public interface PaymentClient {

	@PostMapping("/internal/payments/booking/{bookingId}/close")
	ApiResponse closePaymentOrder(
			@PathVariable("bookingId") Long bookingId,
			@RequestHeader("X-Internal-Api-Key") String internalApiKey
	);
}
