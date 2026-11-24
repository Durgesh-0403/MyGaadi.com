package com.mygaadi.payment.dtos;

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
	private String paymentStatus;
	private String razorpayPaymentId;
}
