package com.mygaadi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mygaadi.dtos.ApiResponse;
import com.mygaadi.dtos.AuthRequest;
import com.mygaadi.dtos.AuthResponse;
import com.mygaadi.dtos.BuyerRegisterDTO;
import com.mygaadi.dtos.JwtDTO;
import com.mygaadi.dtos.SellerRegisterDTO;
import com.mygaadi.dtos.TokenRefreshRequestDTO;
import com.mygaadi.dtos.TokenRefreshResponseDTO;
import com.mygaadi.dtos.UserDTO;
import com.mygaadi.service.AuthService;
import com.mygaadi.service.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration and authentication endpoints")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;

	@PostMapping("/register/buyer")
	@Operation(summary = "Register a new buyer")
	public ResponseEntity<AuthResponse> registerBuyer(@Valid @RequestBody BuyerRegisterDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerBuyer(dto));
	}

	@PostMapping("/register/seller")
	@Operation(summary = "Register a new seller with showroom details")
	public ResponseEntity<AuthResponse> registerSeller(@Valid @RequestBody SellerRegisterDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerSeller(dto));
	}

	@PostMapping("/login")
	@Operation(summary = "Consumer login (Buyers & Sellers)")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/admin/login")
	@Operation(summary = "Dedicated Administrator Login")
	public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AuthRequest request) {
		return ResponseEntity.ok(authService.adminLogin(request));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh JWT access token using valid refresh token")
	public ResponseEntity<TokenRefreshResponseDTO> refresh(@Valid @RequestBody TokenRefreshRequestDTO request) {
		return ResponseEntity.ok(refreshTokenService.refreshAccessToken(request));
	}

	@PostMapping("/admin/refresh")
	@Operation(summary = "Dedicated Admin refresh token endpoint")
	public ResponseEntity<TokenRefreshResponseDTO> adminRefresh(@Valid @RequestBody TokenRefreshRequestDTO request) {
		return ResponseEntity.ok(refreshTokenService.refreshAdminAccessToken(request));
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke refresh token on logout")
	public ResponseEntity<ApiResponse> logout(@Valid @RequestBody TokenRefreshRequestDTO request) {
		refreshTokenService.revokeRefreshToken(request.getRefreshToken());
		return ResponseEntity.ok(new ApiResponse("SUCCESS", "Logged out successfully"));
	}

	@GetMapping("/me")
	@Operation(summary = "Get current authenticated user profile")
	public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal JwtDTO jwtDTO) {
		return ResponseEntity.ok(authService.getCurrentUser(jwtDTO.getUserId()));
	}
}
