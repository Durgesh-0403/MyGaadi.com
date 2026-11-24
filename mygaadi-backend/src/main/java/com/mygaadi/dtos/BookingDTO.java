package com.mygaadi.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class BookingDTO {
	private Long id;
	private String bookingNumber;
	private Long carId;
	private String carTitle;
	private String carBrand;
	private String carModel;
	private BigDecimal carPrice;
	private String primaryImageUrl;
	private Long buyerId;
	private String buyerName;
	private String buyerEmail;
	private String buyerPhone;
	private Long sellerId;
	private String sellerName;
	private String sellerShowroom;
	private BigDecimal tokenAmount;
	private BookingStatus status;
	private PaymentStatus paymentStatus;
	private LocalDateTime expiresAt;
	private LocalDate createdOn;
	private Long version;
}
