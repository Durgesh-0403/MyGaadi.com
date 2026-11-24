package com.mygaadi.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mygaadi.custom_exceptions.InvalidInputException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.ApiResponse;
import com.mygaadi.dtos.BookingDTO;
import com.mygaadi.dtos.CarCreateDTO;
import com.mygaadi.dtos.CarDTO;
import com.mygaadi.dtos.JwtDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.dtos.SellerProfileDTO;
import com.mygaadi.entities.SellerProfile;
import com.mygaadi.repository.SellerProfileRepository;
import com.mygaadi.service.BookingService;
import com.mygaadi.service.CarService;
import com.mygaadi.service.storage.FileStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seller")
@RequiredArgsConstructor
@Tag(name = "Seller Operations", description = "Endpoints restricted to registered sellers")
public class SellerController {

	private final CarService carService;
	private final BookingService bookingService;
	private final SellerProfileRepository sellerProfileRepository;
	private final FileStorageService fileStorageService;
	private final ModelMapper modelMapper;

	@GetMapping("/kyc")
	@Operation(summary = "Get current seller KYC status and submitted document details")
	public ResponseEntity<SellerProfileDTO> getMyKyc(@AuthenticationPrincipal JwtDTO jwtDTO) {
		SellerProfile profile = sellerProfileRepository.findById(jwtDTO.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
		return ResponseEntity.ok(modelMapper.map(profile, SellerProfileDTO.class));
	}

	@PostMapping(value = "/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Submit or update seller KYC documents for admin verification")
	public ResponseEntity<SellerProfileDTO> submitKyc(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@RequestParam("documentType") String documentType,
			@RequestParam("documentNumber") String documentNumber,
			@RequestPart("file") MultipartFile file) {

		if (!"PAN".equalsIgnoreCase(documentType) && !"AADHAAR".equalsIgnoreCase(documentType)) {
			throw new InvalidInputException("Document type must be PAN or AADHAAR");
		}
		if (documentNumber == null || documentNumber.trim().isEmpty()) {
			throw new InvalidInputException("Document number is required");
		}

		SellerProfile profile = sellerProfileRepository.findById(jwtDTO.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

		String fileUrl = fileStorageService.storeKycDocument(file, jwtDTO.getUserId());

		profile.setKycDocumentType(documentType.trim().toUpperCase());
		profile.setKycDocumentNumber(documentNumber.trim().toUpperCase());
		profile.setKycDocumentUrl(fileUrl);
		profile.setKycStatus("PENDING");
		profile.setKycSubmittedAt(LocalDateTime.now());
		profile.setKycRejectionReason(null);

		SellerProfile saved = sellerProfileRepository.save(profile);
		return ResponseEntity.ok(modelMapper.map(saved, SellerProfileDTO.class));
	}

	@PostMapping(value = "/cars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Create car listing with 3-section images (Primary, Exterior, Interior)")
	public ResponseEntity<CarDTO> createCar(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@Valid @RequestPart("car") CarCreateDTO carDTO,
			@RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
			@RequestPart(value = "exteriorImages", required = false) List<MultipartFile> exteriorImages,
			@RequestPart(value = "interiorImages", required = false) List<MultipartFile> interiorImages,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(carService.createCarWithSections(carDTO, primaryImage, exteriorImages, interiorImages, images, jwtDTO.getUserId()));
	}

	@PutMapping("/cars/{id}")
	@Operation(summary = "Update car listing details")
	public ResponseEntity<CarDTO> updateCar(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id,
			@Valid @RequestBody CarCreateDTO carDTO) {
		return ResponseEntity.ok(carService.updateCar(id, carDTO, jwtDTO.getUserId()));
	}

	@DeleteMapping("/cars/{id}")
	@Operation(summary = "Soft delete car listing")
	public ResponseEntity<ApiResponse> deleteCar(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id) {
		carService.deleteCar(id, jwtDTO.getUserId());
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Listing deleted successfully"));
	}

	@PostMapping(value = "/cars/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload additional images to car listing")
	public ResponseEntity<CarDTO> addImages(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id,
			@RequestPart("images") List<MultipartFile> images) {
		return ResponseEntity.ok(carService.addImagesToCar(id, images, jwtDTO.getUserId()));
	}

	@DeleteMapping("/cars/{id}/images/{imageId}")
	@Operation(summary = "Delete an image from car listing")
	public ResponseEntity<CarDTO> deleteImage(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id,
			@PathVariable Long imageId) {
		return ResponseEntity.ok(carService.deleteCarImage(id, imageId, jwtDTO.getUserId()));
	}

	@PatchMapping("/cars/{id}/images/{imageId}/primary")
	@Operation(summary = "Set primary display image for car listing")
	public ResponseEntity<CarDTO> setPrimaryImage(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id,
			@PathVariable Long imageId) {
		return ResponseEntity.ok(carService.setPrimaryCarImage(id, imageId, jwtDTO.getUserId()));
	}

	@GetMapping("/cars")
	@Operation(summary = "Get seller's own cars across all statuses")
	public ResponseEntity<PageResponse<CarDTO>> getMyCars(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(carService.getSellerCars(jwtDTO.getUserId(), page, size));
	}

	@GetMapping("/bookings")
	@Operation(summary = "Get bookings placed on seller's cars")
	public ResponseEntity<PageResponse<BookingDTO>> getSellerBookings(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(bookingService.getSellerBookings(jwtDTO.getUserId(), page, size));
	}

	@RequestMapping(value = "/bookings/{id}/complete", method = {RequestMethod.PATCH, RequestMethod.POST})
	@Operation(summary = "Complete booking transaction (car becomes SOLD)")
	public ResponseEntity<ApiResponse> completeBooking(
			@AuthenticationPrincipal JwtDTO jwtDTO,
			@PathVariable Long id) {
		bookingService.completeBooking(jwtDTO.getUserId(), id);
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Booking marked as COMPLETED; Car is now marked SOLD"));
	}
}
