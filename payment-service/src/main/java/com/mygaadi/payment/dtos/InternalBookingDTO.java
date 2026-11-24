package com.mygaadi.payment.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class InternalBookingDTO {
	private Long id;
	private String bookingNumber;
	private BigDecimal tokenAmount;
	private String status;
	private String paymentStatus;
	private Long buyerId;
	private String buyerName;
	private String buyerEmail;
	private String buyerPhone;
	private LocalDateTime expiresAt;
}
