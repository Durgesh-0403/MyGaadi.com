package com.mygaadi.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mygaadi.entities.BookingStatus;
import com.mygaadi.entities.PaymentStatus;

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
	private BookingStatus status;
	private PaymentStatus paymentStatus;
	private Long buyerId;
	private String buyerName;
	private String buyerEmail;
	private String buyerPhone;
	private LocalDateTime expiresAt;
}
