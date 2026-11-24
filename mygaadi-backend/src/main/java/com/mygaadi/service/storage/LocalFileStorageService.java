package com.mygaadi.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mygaadi.custom_exceptions.ApiException;
import com.mygaadi.custom_exceptions.InvalidInputException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

	@Value("${app.upload.dir:./uploads}")
	private String uploadDir;

	private Path rootLocation;

	@PostConstruct
	public void init() {
		try {
			this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
			Files.createDirectories(this.rootLocation);
			log.info("Initialized local file storage at {}", this.rootLocation);
		} catch (IOException e) {
			throw new ApiException("Could not initialize storage directory: " + e.getMessage());
		}
	}

	@Override
	public String storeCarImage(MultipartFile file, Long carId) {
		if (file == null || file.isEmpty()) {
			throw new InvalidInputException("Cannot upload empty file");
		}

		if (file.getSize() > 5 * 1024 * 1024) {
			throw new InvalidInputException("Image file size exceeds maximum limit of 5 MB");
		}

		String extension = validateAndGetExtension(file);

		try {
			Path carDir = this.rootLocation.resolve("cars").resolve(String.valueOf(carId)).normalize();
			if (!carDir.startsWith(this.rootLocation)) {
				throw new InvalidInputException("Path traversal attempt detected");
			}
			Files.createDirectories(carDir);

			String filename = UUID.randomUUID().toString() + "." + extension;
			Path destinationFile = carDir.resolve(filename).normalize();

			if (!destinationFile.startsWith(carDir)) {
				throw new InvalidInputException("Cannot store file outside target directory");
			}

			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			}

			return "/uploads/cars/" + carId + "/" + filename;

		} catch (IOException e) {
			log.error("Failed to store file for car {}", carId, e);
			throw new ApiException("Failed to save image file: " + e.getMessage());
		}
	}

	@Override
	public String storeKycDocument(MultipartFile file, Long sellerId) {
		if (file == null || file.isEmpty()) {
			throw new InvalidInputException("KYC document file is required");
		}

		if (file.getSize() > 10 * 1024 * 1024) {
			throw new InvalidInputException("KYC document file size exceeds maximum limit of 10 MB");
		}

		String extension = validateAndGetKycExtension(file);

		try {
			Path kycDir = this.rootLocation.resolve("kyc").resolve(String.valueOf(sellerId)).normalize();
			if (!kycDir.startsWith(this.rootLocation)) {
				throw new InvalidInputException("Path traversal attempt detected");
			}
			Files.createDirectories(kycDir);

			String filename = UUID.randomUUID().toString() + "." + extension;
			Path destinationFile = kycDir.resolve(filename).normalize();

			if (!destinationFile.startsWith(kycDir)) {
				throw new InvalidInputException("Cannot store file outside target directory");
			}

			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			}

			log.info("Stored KYC document for seller {} at {}", sellerId, destinationFile);
			return "/uploads/kyc/" + sellerId + "/" + filename;

		} catch (IOException e) {
			log.error("Failed to store KYC document for seller {}", sellerId, e);
			throw new ApiException("Failed to save KYC document file: " + e.getMessage());
		}
	}

	@Override
	public void deleteImage(String fileUrl) {
		if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
			return;
		}

		try {
			String relativePath = fileUrl.substring("/uploads/".length());
			Path filePath = this.rootLocation.resolve(relativePath).normalize();

			if (!filePath.startsWith(this.rootLocation)) {
				log.warn("Blocked potential path traversal delete: {}", fileUrl);
				return;
			}

			Files.deleteIfExists(filePath);
			log.info("Deleted image file: {}", filePath);
		} catch (IOException e) {
			log.warn("Could not delete file for URL {}: {}", fileUrl, e.getMessage());
		}
	}

	@Override
	public Resource loadFileAsResource(String relativePath) {
		try {
			Path filePath = this.rootLocation.resolve(relativePath).normalize();
			if (!filePath.startsWith(this.rootLocation)) {
				throw new ResourceNotFoundException("File not found");
			}

			Resource resource = new UrlResource(filePath.toUri());
			if (resource.exists() && resource.isReadable()) {
				return resource;
			} else {
				throw new ResourceNotFoundException("File not found or unreadable: " + relativePath);
			}
		} catch (MalformedURLException e) {
			throw new ResourceNotFoundException("File not found: " + relativePath);
		}
	}

	private String validateAndGetExtension(MultipartFile file) {
		String contentType = file.getContentType();
		byte[] magicBytes = new byte[12];

		try (InputStream is = file.getInputStream()) {
			int bytesRead = is.read(magicBytes);
			if (bytesRead < 4) {
				throw new InvalidInputException("Invalid file: corrupted or incomplete header");
			}
		} catch (IOException e) {
			throw new InvalidInputException("Could not read file header");
		}

		// JPEG: FF D8 FF
		if ((magicBytes[0] & 0xFF) == 0xFF && (magicBytes[1] & 0xFF) == 0xD8 && (magicBytes[2] & 0xFF) == 0xFF) {
			if (contentType != null && (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/jpg"))) {
				return "jpg";
			}
		}

		// PNG: 89 50 4E 47 0D 0A 1A 0A
		if ((magicBytes[0] & 0xFF) == 0x89 && (magicBytes[1] & 0xFF) == 0x50 &&
			(magicBytes[2] & 0xFF) == 0x4E && (magicBytes[3] & 0xFF) == 0x47) {
			if (contentType != null && contentType.equalsIgnoreCase("image/png")) {
				return "png";
			}
		}

		// WEBP: RIFF....WEBP
		if (magicBytes[0] == 'R' && magicBytes[1] == 'I' && magicBytes[2] == 'F' && magicBytes[3] == 'F' &&
			magicBytes[8] == 'W' && magicBytes[9] == 'E' && magicBytes[10] == 'B' && magicBytes[11] == 'P') {
			if (contentType != null && contentType.equalsIgnoreCase("image/webp")) {
				return "webp";
			}
		}

		throw new InvalidInputException("Unsupported file type. Only JPEG, PNG, and WEBP images are allowed.");
	}

	private String validateAndGetKycExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
		String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
		byte[] magicBytes = new byte[12];

		try (InputStream is = file.getInputStream()) {
			int bytesRead = is.read(magicBytes);
			if (bytesRead < 4) {
				throw new InvalidInputException("Invalid KYC file: corrupted or incomplete header");
			}
		} catch (IOException e) {
			throw new InvalidInputException("Could not read KYC file header");
		}

		// PDF: %PDF (% = 0x25, P = 0x50, D = 0x44, F = 0x46)
		if (magicBytes[0] == 0x25 && magicBytes[1] == 0x50 && magicBytes[2] == 0x44 && magicBytes[3] == 0x46) {
			return "pdf";
		}

		// JPEG: FF D8 FF
		if ((magicBytes[0] & 0xFF) == 0xFF && (magicBytes[1] & 0xFF) == 0xD8 && (magicBytes[2] & 0xFF) == 0xFF) {
			return "jpg";
		}

		// PNG: 89 50 4E 47
		if ((magicBytes[0] & 0xFF) == 0x89 && (magicBytes[1] & 0xFF) == 0x50 &&
			(magicBytes[2] & 0xFF) == 0x4E && (magicBytes[3] & 0xFF) == 0x47) {
			return "png";
		}

		// Fallback to checking extension if file begins with clean bytes
		if (originalFilename.endsWith(".pdf") || contentType.contains("pdf")) {
			return "pdf";
		}
		if (originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg") || contentType.contains("jpeg")) {
			return "jpg";
		}
		if (originalFilename.endsWith(".png") || contentType.contains("png")) {
			return "png";
		}

		throw new InvalidInputException("Unsupported KYC file type. Please upload a PDF, JPEG, or PNG document.");
	}
}
