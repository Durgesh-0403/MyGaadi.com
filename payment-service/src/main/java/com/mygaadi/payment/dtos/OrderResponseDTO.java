package com.mygaadi.payment.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderResponseDTO {
	private Long paymentId;
	private String razorpayOrderId;
	private String razorpayKeyId;
	private BigDecimal amount;
	private String currency;
	private String bookingNumber;
	private PaymentPrefillDTO prefill;
}
