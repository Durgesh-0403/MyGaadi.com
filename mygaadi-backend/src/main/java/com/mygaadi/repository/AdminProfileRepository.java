package com.mygaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mygaadi.entities.AdminProfile;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
}
