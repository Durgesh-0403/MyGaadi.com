package com.mygaadi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mygaadi.entities.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

	@Query("SELECT w FROM WishlistItem w JOIN FETCH w.car c LEFT JOIN FETCH c.images WHERE w.user.id = :userId AND w.deleted = false AND c.deleted = false")
	List<WishlistItem> findActiveWishlistByUserId(@Param("userId") Long userId);

	Optional<WishlistItem> findByUserIdAndCarId(Long userId, Long carId);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.transaction.annotation.Transactional
	@Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);
}
