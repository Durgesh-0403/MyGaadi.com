package com.mygaadi.payment.entities;

import java.math.BigDecimal;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "payments")
@AttributeOverride(name = "id", column = @Column(name = "payment_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Payment extends BaseClass {

	@Column(name = "booking_id", nullable = false)
	private Long bookingId;

	@Column(name = "booking_number", length = 50, nullable = false)
	private String bookingNumber;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "amount", precision = 12, scale = 2, nullable = false)
	private BigDecimal amount;

	@Column(name = "currency", length = 10, nullable = false)
	@Builder.Default
	private String currency = "INR";

	@Column(name = "razorpay_order_id", length = 100, nullable = false, unique = true)
	private String razorpayOrderId;

	@Column(name = "razorpay_payment_id", length = 100, unique = true)
	private String razorpayPaymentId;

	@Column(name = "razorpay_signature", length = 255)
	private String razorpaySignature;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	@Builder.Default
	private PaymentRecordStatus status = PaymentRecordStatus.CREATED;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	@Column(name = "method", length = 50)
	private String method;

	@Column(name = "razorpay_refund_id", length = 100)
	private String razorpayRefundId;

	@Column(name = "backend_notified", nullable = false)
	@Builder.Default
	private boolean backendNotified = false;

	@Version
	@Column(name = "version")
	private Long version;
}
