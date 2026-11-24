package com.mygaadi.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.mygaadi.dtos.CarCreateDTO;
import com.mygaadi.dtos.CarDTO;
import com.mygaadi.dtos.CarFilterOptionsDTO;
import com.mygaadi.dtos.InternalCarExportDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.Transmission;

public interface CarService {

	PageResponse<CarDTO> searchCars(
			String q,
			String brand,
			String model,
			FuelType fuelType,
			Transmission transmission,
			BodyType bodyType,
			String city,
			String state,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Integer minYear,
			Integer maxYear,
			Integer maxKm,
			Integer maxOwners,
			int page,
			int size,
			String sortBy,
			String direction
	);

	CarDTO getCarById(Long id);

	CarFilterOptionsDTO getFilterOptions();

	CarDTO createCar(CarCreateDTO dto, List<MultipartFile> images, Long sellerId);

	CarDTO createCarWithSections(CarCreateDTO dto, MultipartFile primaryImage, List<MultipartFile> exteriorImages, List<MultipartFile> interiorImages, List<MultipartFile> images, Long sellerId);

	CarDTO updateCar(Long carId, CarCreateDTO dto, Long sellerId);

	void deleteCar(Long carId, Long sellerId);

	CarDTO addImagesToCar(Long carId, List<MultipartFile> images, Long sellerId);

	CarDTO deleteCarImage(Long carId, Long imageId, Long sellerId);

	CarDTO setPrimaryCarImage(Long carId, Long imageId, Long sellerId);

	PageResponse<CarDTO> getSellerCars(Long sellerId, int page, int size);

	PageResponse<CarDTO> getAdminCars(ApprovalStatus approvalStatus, Boolean deleted, String q, int page, int size);

	CarDTO approveCar(Long carId);

	CarDTO rejectCar(Long carId, String reason);

	void adminDeleteCar(Long carId);

	CarDTO restoreCar(Long carId);

	PageResponse<InternalCarExportDTO> exportCarsForRag(int page, int size);
}
