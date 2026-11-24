package com.mygaadi.payment.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mygaadi.payment.entities.Payment;
import com.mygaadi.payment.entities.PaymentRecordStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

	Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

	Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedOnDesc(Long bookingId, PaymentRecordStatus status);

	Optional<Payment> findFirstByBookingIdOrderByCreatedOnDesc(Long bookingId);

	Page<Payment> findByBuyerId(Long buyerId, Pageable pageable);

	Page<Payment> findByStatus(PaymentRecordStatus status, Pageable pageable);

	List<Payment> findByBackendNotifiedFalseAndStatusIn(List<PaymentRecordStatus> statuses);

	@Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
	BigDecimal sumAmountByStatus(@Param("status") PaymentRecordStatus status);
}
