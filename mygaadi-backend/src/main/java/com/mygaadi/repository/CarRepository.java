package com.mygaadi.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.Car;
import com.mygaadi.entities.CarStatus;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

	Optional<Car> findByIdAndDeletedFalse(Long id);

	Page<Car> findBySellerIdAndDeletedFalse(Long sellerId, Pageable pageable);

	@Query("SELECT c FROM Car c WHERE " +
		   "(:approvalStatus IS NULL OR c.approvalStatus = :approvalStatus) AND " +
		   "(:deleted IS NULL OR c.deleted = :deleted) AND " +
		   "(:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
		   "LOWER(c.brand) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
		   "LOWER(c.model) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
		   "LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')))")
	Page<Car> searchAdminCars(@Param("approvalStatus") ApprovalStatus approvalStatus,
							  @Param("deleted") Boolean deleted,
							  @Param("q") String q,
							  Pageable pageable);

	long countByApprovalStatusAndDeletedFalse(ApprovalStatus approvalStatus);

	long countByStatusAndDeletedFalse(CarStatus status);

	@Query("SELECT DISTINCT c.brand FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.deleted = false AND c.seller.sellerProfile.verified = true ORDER BY c.brand ASC")
	List<String> findDistinctBrands();

	@Query("SELECT DISTINCT c.model FROM Car c WHERE c.brand = :brand AND c.approvalStatus = 'APPROVED' AND c.deleted = false AND c.seller.sellerProfile.verified = true ORDER BY c.model ASC")
	List<String> findDistinctModelsByBrand(@Param("brand") String brand);

	@Query("SELECT DISTINCT c.city FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.deleted = false AND c.seller.sellerProfile.verified = true ORDER BY c.city ASC")
	List<String> findDistinctCities();

	@Query("SELECT MIN(c.price) FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.status = 'AVAILABLE' AND c.deleted = false AND c.seller.sellerProfile.verified = true")
	BigDecimal findMinPrice();

	@Query("SELECT MAX(c.price) FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.status = 'AVAILABLE' AND c.deleted = false AND c.seller.sellerProfile.verified = true")
	BigDecimal findMaxPrice();

	@Query("SELECT MIN(c.manufactureYear) FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.status = 'AVAILABLE' AND c.deleted = false AND c.seller.sellerProfile.verified = true")
	Integer findMinYear();

	@Query("SELECT MAX(c.manufactureYear) FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.status = 'AVAILABLE' AND c.deleted = false AND c.seller.sellerProfile.verified = true")
	Integer findMaxYear();

	@Query("SELECT c FROM Car c WHERE c.approvalStatus = 'APPROVED' AND c.status = 'AVAILABLE' AND c.deleted = false AND c.seller.sellerProfile.verified = true")
	Page<Car> findApprovedExportCars(Pageable pageable);

	List<Car> findBySellerId(Long sellerId);
}
