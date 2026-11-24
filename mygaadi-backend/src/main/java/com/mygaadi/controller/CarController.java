package com.mygaadi.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.dtos.CarDTO;
import com.mygaadi.dtos.CarFilterOptionsDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.Transmission;
import com.mygaadi.service.CarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
@Tag(name = "Public Cars", description = "Public endpoints for browsing and searching cars")
public class CarController {

	private final CarService carService;

	@GetMapping
	@Operation(summary = "Search cars with filtering, sorting and pagination")
	public ResponseEntity<PageResponse<CarDTO>> searchCars(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String brand,
			@RequestParam(required = false) String model,
			@RequestParam(required = false) FuelType fuelType,
			@RequestParam(required = false) Transmission transmission,
			@RequestParam(required = false) BodyType bodyType,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) Integer minYear,
			@RequestParam(required = false) Integer maxYear,
			@RequestParam(required = false) Integer maxKm,
			@RequestParam(required = false) Integer maxOwners,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size,
			@RequestParam(defaultValue = "createdOn") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		PageResponse<CarDTO> response = carService.searchCars(
				q, brand, model, fuelType, transmission, bodyType, city, state,
				minPrice, maxPrice, minYear, maxYear, maxKm, maxOwners,
				page, size, sortBy, direction
		);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get detailed specification for a single car")
	public ResponseEntity<CarDTO> getCarById(@PathVariable Long id) {
		return ResponseEntity.ok(carService.getCarById(id));
	}

	@GetMapping("/filter-options")
	@Operation(summary = "Get dynamic filter options for cars search")
	public ResponseEntity<CarFilterOptionsDTO> getFilterOptions() {
		return ResponseEntity.ok(carService.getFilterOptions());
	}
}
