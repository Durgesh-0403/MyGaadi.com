package com.mygaadi.service;

import com.mygaadi.dtos.TokenRefreshRequestDTO;
import com.mygaadi.dtos.TokenRefreshResponseDTO;
import com.mygaadi.entities.RefreshToken;

public interface RefreshTokenService {

	RefreshToken createRefreshToken(Long userId);

	TokenRefreshResponseDTO refreshAccessToken(TokenRefreshRequestDTO request);

	TokenRefreshResponseDTO refreshAdminAccessToken(TokenRefreshRequestDTO request);

	void revokeRefreshToken(String token);

	void revokeAllUserTokens(Long userId);
}
