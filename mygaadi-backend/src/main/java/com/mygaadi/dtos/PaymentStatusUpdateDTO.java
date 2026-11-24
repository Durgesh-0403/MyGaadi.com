package com.mygaadi.dtos;

import com.mygaadi.entities.PaymentStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentStatusUpdateDTO {

	@NotNull(message = "Payment status is required")
	private PaymentStatus paymentStatus;

	private String razorpayPaymentId;
}
