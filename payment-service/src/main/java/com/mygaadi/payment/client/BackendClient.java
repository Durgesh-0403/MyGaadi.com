package com.mygaadi.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mygaadi.payment.dtos.ApiResponse;
import com.mygaadi.payment.dtos.InternalBookingDTO;
import com.mygaadi.payment.dtos.PaymentStatusUpdateDTO;

@FeignClient(name = "backend-service", url = "${backend.service.url:http://localhost:8081}", configuration = FeignClientConfig.class)
public interface BackendClient {

	@GetMapping("/internal/bookings/{id}")
	InternalBookingDTO getBookingById(@PathVariable("id") Long id);

	@PutMapping("/internal/bookings/{id}/payment-status")
	ApiResponse updatePaymentStatus(@PathVariable("id") Long id, @RequestBody PaymentStatusUpdateDTO dto);
}
