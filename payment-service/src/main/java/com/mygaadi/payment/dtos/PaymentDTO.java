package com.mygaadi.payment.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mygaadi.payment.entities.PaymentRecordStatus;

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
public class PaymentDTO {
	private Long id;
	private Long bookingId;
	private String bookingNumber;
	private Long buyerId;
	private BigDecimal amount;
	private String currency;
	private String razorpayOrderId;
	private String razorpayPaymentId;
	private PaymentRecordStatus status;
	private String failureReason;
	private String method;
	private String razorpayRefundId;
	private boolean backendNotified;
	private LocalDate createdOn;
	private LocalDateTime lastUpdated;
}
