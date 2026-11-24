package com.mygaadi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mygaadi.custom_exceptions.ApiException;
import com.mygaadi.custom_exceptions.InvalidInputException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.CarCreateDTO;
import com.mygaadi.dtos.CarDTO;
import com.mygaadi.dtos.CarFilterOptionsDTO;
import com.mygaadi.dtos.CarImageDTO;
import com.mygaadi.dtos.InternalCarExportDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.Car;
import com.mygaadi.entities.CarImage;
import com.mygaadi.entities.CarStatus;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.SellerProfile;
import com.mygaadi.entities.Transmission;
import com.mygaadi.entities.User;
import com.mygaadi.repository.CarImageRepository;
import com.mygaadi.repository.CarRepository;
import com.mygaadi.repository.SellerProfileRepository;
import com.mygaadi.repository.UserRepository;
import com.mygaadi.repository.specifications.CarSpecification;
import com.mygaadi.service.storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CarServiceImpl implements CarService {

	private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdOn", "price", "manufactureYear", "kmDriven");
	private static final int MAX_IMAGES_PER_CAR = 25;

	private final CarRepository carRepository;
	private final CarImageRepository carImageRepository;
	private final UserRepository userRepository;
	private final SellerProfileRepository sellerProfileRepository;
	private final FileStorageService fileStorageService;
	private final ModelMapper modelMapper;

	@Override
	public PageResponse<CarDTO> searchCars(
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
			String direction) {

		int clampedSize = Math.min(Math.max(1, size), 50);
		int validPage = Math.max(0, page);

		String sortField = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdOn";
		Sort.Direction sortDir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
		Pageable pageable = PageRequest.of(validPage, clampedSize, Sort.by(sortDir, sortField));

		Specification<Car> spec = CarSpecification.getFilterSpec(
				q, brand, model, fuelType, transmission, bodyType, city, state,
				minPrice, maxPrice, minYear, maxYear, maxKm, maxOwners
		);

		Page<Car> carPage = carRepository.findAll(spec, pageable);
		List<CarDTO> dtoList = carPage.getContent().stream()
				.map(this::mapToCarDTO)
				.collect(Collectors.toList());

		return PageResponse.of(carPage, dtoList);
	}

	@Override
	public CarDTO getCarById(Long id) {
		Car car = carRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));

		return mapToCarDTO(car);
	}

	@Override
	public CarFilterOptionsDTO getFilterOptions() {
		List<String> brands = carRepository.findDistinctBrands();
		Map<String, List<String>> brandModelsMap = new HashMap<>();

		for (String b : brands) {
			List<String> models = carRepository.findDistinctModelsByBrand(b);
			brandModelsMap.put(b, models);
		}

		List<String> cities = carRepository.findDistinctCities();
		BigDecimal minPrice = carRepository.findMinPrice();
		BigDecimal maxPrice = carRepository.findMaxPrice();
		Integer minYear = carRepository.findMinYear();
		Integer maxYear = carRepository.findMaxYear();

		return CarFilterOptionsDTO.builder()
				.brands(brands)
				.brandModelsMap(brandModelsMap)
				.cities(cities)
				.fuelTypes(Arrays.asList(FuelType.values()))
				.transmissions(Arrays.asList(Transmission.values()))
				.bodyTypes(Arrays.asList(BodyType.values()))
				.minPrice(minPrice != null ? minPrice : BigDecimal.valueOf(50000))
				.maxPrice(maxPrice != null ? maxPrice : BigDecimal.valueOf(10000000))
				.minYear(minYear != null ? minYear : 2005)
				.maxYear(maxYear != null ? maxYear : 2026)
				.build();
	}

	@Override
	@Transactional
	public CarDTO createCar(CarCreateDTO dto, List<MultipartFile> images, Long sellerId) {
		return createCarWithSections(dto, null, null, null, images, sellerId);
	}

	@Override
	@Transactional
	public CarDTO createCarWithSections(CarCreateDTO dto, MultipartFile primaryImage, List<MultipartFile> exteriorImages, List<MultipartFile> interiorImages, List<MultipartFile> images, Long sellerId) {
		User seller = userRepository.findById(sellerId)
				.orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

		if (!seller.isActive()) {
			throw new AccessDeniedException("Your account is blocked. You cannot list cars.");
		}

		SellerProfile sellerProfile = sellerProfileRepository.findById(sellerId).orElse(null);
		if (sellerProfile == null || !sellerProfile.isVerified()) {
			throw new ApiException("KYC verification required. You must submit your KYC (PAN/Aadhaar) and receive administrator approval before listing cars for sale.");
		}

		boolean hasPrimary = primaryImage != null && !primaryImage.isEmpty();
		boolean hasExterior = exteriorImages != null && !exteriorImages.isEmpty();
		boolean hasInterior = interiorImages != null && !interiorImages.isEmpty();
		boolean hasGeneric = images != null && !images.isEmpty();

		if (!hasPrimary && !hasExterior && !hasInterior && !hasGeneric) {
			throw new InvalidInputException("At least one image is required to create a listing");
		}

		Car car = Car.builder()
				.title(dto.getTitle().trim())
				.brand(dto.getBrand().trim())
				.model(dto.getModel().trim())
				.variant(dto.getVariant().trim())
				.manufactureYear(dto.getManufactureYear())
				.price(dto.getPrice())
				.kmDriven(dto.getKmDriven())
				.fuelType(dto.getFuelType())
				.transmission(dto.getTransmission())
				.bodyType(dto.getBodyType())
				.ownerCount(dto.getOwnerCount())
				.color(dto.getColor().trim())
				.city(dto.getCity().trim())
				.state(dto.getState().trim())
				.description(dto.getDescription())
				.status(CarStatus.AVAILABLE)
				.approvalStatus(ApprovalStatus.PENDING)
				.seller(seller)
				.deleted(false)
				.build();

		Car savedCar = carRepository.save(car);
		int order = 0;

		// 1. Process Section 1: Hero / Original Car Photo
		if (hasPrimary) {
			String imageUrl = fileStorageService.storeCarImage(primaryImage, savedCar.getId());
			String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
			CarImage carImage = CarImage.builder()
					.car(savedCar)
					.fileName(fileName)
					.url(imageUrl)
					.primary(true)
					.category("PRIMARY")
					.displayOrder(order++)
					.build();
			savedCar.addImage(carImage);
		}

		// 2. Process Section 2: Exterior Gallery (Up to 8 images)
		if (hasExterior) {
			int extCount = 0;
			for (MultipartFile file : exteriorImages) {
				if (file != null && !file.isEmpty() && extCount < 8) {
					String imageUrl = fileStorageService.storeCarImage(file, savedCar.getId());
					String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
					CarImage carImage = CarImage.builder()
							.car(savedCar)
							.fileName(fileName)
							.url(imageUrl)
							.primary(!hasPrimary && order == 0)
							.category("EXTERIOR")
							.displayOrder(order++)
							.build();
					savedCar.addImage(carImage);
					extCount++;
				}
			}
		}

		// 3. Process Section 3: Interior Gallery (Up to 8 images)
		if (hasInterior) {
			int intCount = 0;
			for (MultipartFile file : interiorImages) {
				if (file != null && !file.isEmpty() && intCount < 8) {
					String imageUrl = fileStorageService.storeCarImage(file, savedCar.getId());
					String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
					CarImage carImage = CarImage.builder()
							.car(savedCar)
							.fileName(fileName)
							.url(imageUrl)
							.primary(!hasPrimary && !hasExterior && order == 0)
							.category("INTERIOR")
							.displayOrder(order++)
							.build();
					savedCar.addImage(carImage);
					intCount++;
				}
			}
		}

		// 4. Fallback for generic images list if sections weren't used
		if (!hasPrimary && !hasExterior && !hasInterior && hasGeneric) {
			for (int i = 0; i < images.size() && i < MAX_IMAGES_PER_CAR; i++) {
				MultipartFile file = images.get(i);
				String imageUrl = fileStorageService.storeCarImage(file, savedCar.getId());
				String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
				CarImage carImage = CarImage.builder()
						.car(savedCar)
						.fileName(fileName)
						.url(imageUrl)
						.primary(i == 0)
						.category(i == 0 ? "PRIMARY" : "EXTERIOR")
						.displayOrder(order++)
						.build();
				savedCar.addImage(carImage);
			}
		}

		Car finalCar = carRepository.save(savedCar);
		log.info("Seller {} listed car '{}' with id {}", seller.getEmail(), finalCar.getTitle(), finalCar.getId());

		return mapToCarDTO(finalCar);
	}

	@Override
	@Transactional
	public CarDTO updateCar(Long carId, CarCreateDTO dto, Long sellerId) {
		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (!car.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this listing");
		}

		car.setTitle(dto.getTitle().trim());
		car.setBrand(dto.getBrand().trim());
		car.setModel(dto.getModel().trim());
		car.setVariant(dto.getVariant().trim());
		car.setManufactureYear(dto.getManufactureYear());
		car.setPrice(dto.getPrice());
		car.setKmDriven(dto.getKmDriven());
		car.setFuelType(dto.getFuelType());
		car.setTransmission(dto.getTransmission());
		car.setBodyType(dto.getBodyType());
		car.setOwnerCount(dto.getOwnerCount());
		car.setColor(dto.getColor().trim());
		car.setCity(dto.getCity().trim());
		car.setState(dto.getState().trim());
		car.setDescription(dto.getDescription());

		// Section 5 rule: Editing a REJECTED car sends it back to PENDING
		if (car.getApprovalStatus() == ApprovalStatus.REJECTED) {
			car.setApprovalStatus(ApprovalStatus.PENDING);
			car.setRejectionReason(null);
			log.info("Car {} updated by seller; approval status reverted from REJECTED to PENDING", carId);
		}

		Car updated = carRepository.save(car);
		return mapToCarDTO(updated);
	}

	@Override
	@Transactional
	public void deleteCar(Long carId, Long sellerId) {
		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (!car.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this listing");
		}

		car.setDeleted(true);
		car.setDeletedAt(LocalDateTime.now());
		carRepository.save(car);
		log.info("Seller {} soft-deleted car listing {}", sellerId, carId);
	}

	@Override
	@Transactional
	public CarDTO addImagesToCar(Long carId, List<MultipartFile> images, Long sellerId) {
		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (!car.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this listing");
		}

		int currentCount = car.getImages().size();
		if (currentCount + images.size() > MAX_IMAGES_PER_CAR) {
			throw new InvalidInputException("Adding these images would exceed the 8-image limit. Current: " + currentCount);
		}

		boolean hasPrimary = car.getImages().stream().anyMatch(CarImage::isPrimary);

		for (int i = 0; i < images.size(); i++) {
			MultipartFile file = images.get(i);
			String imageUrl = fileStorageService.storeCarImage(file, car.getId());
			String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

			boolean makePrimary = !hasPrimary && (i == 0);
			CarImage carImage = CarImage.builder()
					.car(car)
					.fileName(fileName)
					.url(imageUrl)
					.primary(makePrimary)
					.displayOrder(currentCount + i)
					.build();

			car.addImage(carImage);
		}

		Car updated = carRepository.save(car);
		return mapToCarDTO(updated);
	}

	@Override
	@Transactional
	public CarDTO deleteCarImage(Long carId, Long imageId, Long sellerId) {
		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (!car.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this listing");
		}

		CarImage targetImage = car.getImages().stream()
				.filter(img -> img.getId().equals(imageId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

		if (car.getImages().size() <= 1) {
			throw new InvalidInputException("A listing must have at least one image");
		}

		boolean wasPrimary = targetImage.isPrimary();
		fileStorageService.deleteImage(targetImage.getUrl());
		car.removeImage(targetImage);

		if (wasPrimary && !car.getImages().isEmpty()) {
			car.getImages().get(0).setPrimary(true);
		}

		Car updated = carRepository.save(car);
		return mapToCarDTO(updated);
	}

	@Override
	@Transactional
	public CarDTO setPrimaryCarImage(Long carId, Long imageId, Long sellerId) {
		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (!car.getSeller().getId().equals(sellerId)) {
			throw new AccessDeniedException("Forbidden: You do not own this listing");
		}

		boolean found = false;
		for (CarImage img : car.getImages()) {
			if (img.getId().equals(imageId)) {
				img.setPrimary(true);
				found = true;
			} else {
				img.setPrimary(false);
			}
		}

		if (!found) {
			throw new ResourceNotFoundException("Image not found with id: " + imageId);
		}

		Car updated = carRepository.save(car);
		return mapToCarDTO(updated);
	}

	@Override
	public PageResponse<CarDTO> getSellerCars(Long sellerId, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		Page<Car> carPage = carRepository.findBySellerIdAndDeletedFalse(sellerId, pageable);
		List<CarDTO> dtoList = carPage.getContent().stream()
				.map(this::mapToCarDTO)
				.collect(Collectors.toList());

		return PageResponse.of(carPage, dtoList);
	}

	@Override
	public PageResponse<CarDTO> getAdminCars(ApprovalStatus approvalStatus, Boolean deleted, String q, int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 50);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "createdOn"));

		String search = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
		Page<Car> carPage = carRepository.searchAdminCars(approvalStatus, deleted, search, pageable);

		List<CarDTO> dtoList = carPage.getContent().stream()
				.map(this::mapToCarDTO)
				.collect(Collectors.toList());

		return PageResponse.of(carPage, dtoList);
	}

	@Override
	@Transactional
	public CarDTO approveCar(Long carId) {
		Car car = carRepository.findById(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (car.getStatus() == CarStatus.SOLD) {
			throw new ApiException("Cannot moderate listing: This vehicle is already sold.");
		}

		SellerProfile sellerProfile = sellerProfileRepository.findById(car.getSeller().getId()).orElse(null);
		if (sellerProfile == null || !sellerProfile.isVerified()) {
			throw new ApiException("Cannot approve listing: The seller's KYC has not been verified by an administrator yet.");
		}

		car.setApprovalStatus(ApprovalStatus.APPROVED);
		car.setRejectionReason(null);
		Car updated = carRepository.save(car);
		log.info("Admin approved car listing: {}", carId);
		return mapToCarDTO(updated);
	}

	@Override
	@Transactional
	public CarDTO rejectCar(Long carId, String reason) {
		Car car = carRepository.findById(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		if (car.getStatus() == CarStatus.SOLD) {
			throw new ApiException("Cannot moderate listing: This vehicle is already sold.");
		}

		car.setApprovalStatus(ApprovalStatus.REJECTED);
		car.setRejectionReason(reason != null ? reason.trim() : "Did not meet marketplace quality standards");
		Car updated = carRepository.save(car);
		log.info("Admin rejected car listing: {} with reason: {}", carId, car.getRejectionReason());
		return mapToCarDTO(updated);
	}

	@Override
	@Transactional
	public void adminDeleteCar(Long carId) {
		Car car = carRepository.findById(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		car.setDeleted(true);
		car.setDeletedAt(LocalDateTime.now());
		carRepository.save(car);
		log.info("Admin soft-deleted car listing: {}", carId);
	}

	@Override
	@Transactional
	public CarDTO restoreCar(Long carId) {
		Car car = carRepository.findById(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		car.setDeleted(false);
		car.setDeletedAt(null);
		Car updated = carRepository.save(car);
		log.info("Admin restored car listing: {}", carId);
		return mapToCarDTO(updated);
	}

	@Override
	public PageResponse<InternalCarExportDTO> exportCarsForRag(int page, int size) {
		int clampedSize = Math.min(Math.max(1, size), 100);
		Pageable pageable = PageRequest.of(Math.max(0, page), clampedSize, Sort.by(Sort.Direction.DESC, "id"));

		Page<Car> carPage = carRepository.findApprovedExportCars(pageable);
		List<InternalCarExportDTO> exportList = carPage.getContent().stream()
				.map(car -> InternalCarExportDTO.builder()
						.id(car.getId())
						.title(car.getTitle())
						.brand(car.getBrand())
						.model(car.getModel())
						.variant(car.getVariant())
						.manufactureYear(car.getManufactureYear())
						.price(car.getPrice())
						.kmDriven(car.getKmDriven())
						.fuelType(car.getFuelType())
						.transmission(car.getTransmission())
						.bodyType(car.getBodyType())
						.ownerCount(car.getOwnerCount())
						.color(car.getColor())
						.city(car.getCity())
						.state(car.getState())
						.description(car.getDescription())
						.status(car.getStatus().name())
						.approvalStatus(car.getApprovalStatus().name())
						.primaryImageUrl(car.getPrimaryImageUrl())
						.build())
				.collect(Collectors.toList());

		return PageResponse.of(carPage, exportList);
	}

	private CarDTO mapToCarDTO(Car car) {
		List<CarImageDTO> imageDTOs = car.getImages().stream()
				.map(img -> CarImageDTO.builder()
						.id(img.getId())
						.fileName(img.getFileName())
						.url(img.getUrl())
						.primary(img.isPrimary())
						.category(img.getCategory() != null ? img.getCategory() : (img.isPrimary() ? "PRIMARY" : "EXTERIOR"))
						.displayOrder(img.getDisplayOrder())
						.build())
				.collect(Collectors.toList());

		User seller = car.getSeller();
		String sellerShowroom = null;
		boolean sellerVerified = false;
		if (seller != null && seller.getSellerProfile() != null) {
			sellerShowroom = seller.getSellerProfile().getShowroomName();
			sellerVerified = seller.getSellerProfile().isVerified();
		}

		return CarDTO.builder()
				.id(car.getId())
				.title(car.getTitle())
				.brand(car.getBrand())
				.model(car.getModel())
				.variant(car.getVariant())
				.manufactureYear(car.getManufactureYear())
				.price(car.getPrice())
				.kmDriven(car.getKmDriven())
				.fuelType(car.getFuelType())
				.transmission(car.getTransmission())
				.bodyType(car.getBodyType())
				.ownerCount(car.getOwnerCount())
				.color(car.getColor())
				.city(car.getCity())
				.state(car.getState())
				.description(car.getDescription())
				.status(car.getStatus())
				.approvalStatus(car.getApprovalStatus())
				.rejectionReason(car.getRejectionReason())
				.sellerId(seller != null ? seller.getId() : null)
				.sellerName(seller != null ? seller.getCompleteName() : null)
				.sellerShowroom(sellerShowroom)
				.sellerVerified(sellerVerified)
				.primaryImageUrl(car.getPrimaryImageUrl())
				.images(imageDTOs)
				.version(car.getVersion())
				.deleted(car.isDeleted())
				.createdOn(car.getCreatedOn())
				.lastUpdated(car.getLastUpdated())
				.build();
	}
}
