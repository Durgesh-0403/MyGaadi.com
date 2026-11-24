package com.mygaadi.service;

import com.mygaadi.dtos.AuthRequest;
import com.mygaadi.dtos.AuthResponse;
import com.mygaadi.dtos.BuyerRegisterDTO;
import com.mygaadi.dtos.SellerRegisterDTO;
import com.mygaadi.dtos.UserDTO;

public interface AuthService {

	AuthResponse registerBuyer(BuyerRegisterDTO dto);

	AuthResponse registerSeller(SellerRegisterDTO dto);

	AuthResponse login(AuthRequest request);

	AuthResponse adminLogin(AuthRequest request);

	UserDTO getCurrentUser(Long userId);
}
