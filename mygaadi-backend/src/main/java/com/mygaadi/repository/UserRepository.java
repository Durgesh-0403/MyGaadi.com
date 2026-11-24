package com.mygaadi.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mygaadi.entities.User;
import com.mygaadi.entities.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);

	long countByUserRole(UserRole userRole);

	@Query("SELECT u FROM User u WHERE " +
		   "(:role IS NULL OR u.userRole = :role) AND " +
		   "(:q IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
		   "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
		   "LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
		   "u.phone LIKE CONCAT('%', :q, '%'))")
	Page<User> searchUsers(@Param("role") UserRole role, @Param("q") String q, Pageable pageable);
}
