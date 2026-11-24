package com.mygaadi.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

	String storeCarImage(MultipartFile file, Long carId);

	String storeKycDocument(MultipartFile file, Long sellerId);

	void deleteImage(String fileUrl);

	Resource loadFileAsResource(String relativePath);
}
