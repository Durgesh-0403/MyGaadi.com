package com.mygaadi.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mygaadi.entities.Booking;
import com.mygaadi.entities.BookingStatus;
import com.mygaadi.entities.PaymentStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	@Query("SELECT b FROM Booking b JOIN FETCH b.car c JOIN FETCH b.buyer u WHERE b.id = :id")
	Optional<Booking> findDetailedById(@Param("id") Long id);

	@Query("SELECT b FROM Booking b JOIN FETCH b.car c JOIN FETCH b.buyer u WHERE b.buyer.id = :buyerId")
	Page<Booking> findByBuyerId(@Param("buyerId") Long buyerId, Pageable pageable);

	@Query("SELECT b FROM Booking b JOIN FETCH b.car c JOIN FETCH b.buyer u WHERE c.seller.id = :sellerId")
	Page<Booking> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

	@Query("SELECT b FROM Booking b JOIN FETCH b.car c JOIN FETCH b.buyer u")
	Page<Booking> findAllDetailed(Pageable pageable);

	List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime expiryThreshold);

	long countByStatus(BookingStatus status);

	boolean existsByBuyerIdAndCarIdAndStatusIn(Long buyerId, Long carId, java.util.Collection<BookingStatus> statuses);

	@Query("SELECT COALESCE(SUM(b.tokenAmount), 0) FROM Booking b WHERE b.paymentStatus = :status")
	BigDecimal sumPaidTokenAmount(@Param("status") PaymentStatus status);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.transaction.annotation.Transactional
	@Query("DELETE FROM Booking b WHERE b.buyer.id = :buyerId")
	void deleteByBuyerId(@Param("buyerId") Long buyerId);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.transaction.annotation.Transactional
	@Query("DELETE FROM Booking b WHERE b.car.id = :carId")
	void deleteByCarId(@Param("carId") Long carId);
}
