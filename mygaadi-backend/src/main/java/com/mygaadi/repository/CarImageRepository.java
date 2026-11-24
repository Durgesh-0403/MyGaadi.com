package com.mygaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mygaadi.entities.CarImage;

public interface CarImageRepository extends JpaRepository<CarImage, Long> {
}
