package com.mygaadi.service;

import com.mygaadi.dtos.AdminDashboardDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.dtos.SellerProfileDTO;
import com.mygaadi.dtos.UserDTO;
import com.mygaadi.entities.UserRole;

public interface AdminService {

	AdminDashboardDTO getDashboardMetrics();

	PageResponse<UserDTO> getUsers(UserRole role, String q, int page, int size);

	void blockUser(Long userId, Long currentAdminId);

	void unblockUser(Long userId);

	void deleteUser(Long userId, Long currentAdminId);

	void verifySeller(Long sellerId);

	SellerProfileDTO updateSellerKycStatus(Long sellerId, String status, String rejectionReason);

	SellerProfileDTO getSellerKyc(Long sellerId);
}
