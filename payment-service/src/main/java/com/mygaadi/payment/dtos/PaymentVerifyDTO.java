package com.mygaadi.payment.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "signature")
public class PaymentVerifyDTO {

	@NotBlank(message = "Razorpay order ID is required")
	private String orderId;

	@NotBlank(message = "Razorpay payment ID is required")
	private String paymentId;

	@NotBlank(message = "Razorpay signature is required")
	private String signature;
}
