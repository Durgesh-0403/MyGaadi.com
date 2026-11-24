package com.mygaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mygaadi.entities.BuyerProfile;

public interface BuyerProfileRepository extends JpaRepository<BuyerProfile, Long> {
}
