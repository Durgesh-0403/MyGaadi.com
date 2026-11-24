package com.mygaadi.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "bookings")
@AttributeOverride(name = "id", column = @Column(name = "booking_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"car", "buyer"})
public class Booking extends BaseClass {

	@Column(name = "booking_number", length = 30, nullable = false, unique = true)
	private String bookingNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "car_id", nullable = false)
	private Car car;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	@Column(name = "token_amount", precision = 12, scale = 2, nullable = false)
	private BigDecimal tokenAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	@Builder.Default
	private BookingStatus status = BookingStatus.PENDING_PAYMENT;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", length = 30, nullable = false)
	@Builder.Default
	private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Version
	@Column(name = "version")
	private Long version;
}
