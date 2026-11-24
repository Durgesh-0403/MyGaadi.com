package com.mygaadi.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.TokenRefreshRequestDTO;
import com.mygaadi.dtos.TokenRefreshResponseDTO;
import com.mygaadi.entities.RefreshToken;
import com.mygaadi.entities.User;
import com.mygaadi.entities.UserRole;
import com.mygaadi.repository.RefreshTokenRepository;
import com.mygaadi.repository.UserRepository;
import com.mygaadi.security.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

	@Value("${jwt.refresh.exp.time:604800000}")
	private long refreshExpTime;

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final JwtUtils jwtUtils;

	@Override
	@Transactional
	public RefreshToken createRefreshToken(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		// Revoke previous tokens for clean session rotation
		refreshTokenRepository.deleteByUser(user);

		String tokenString = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
		RefreshToken refreshToken = RefreshToken.builder()
				.user(user)
				.token(tokenString)
				.expiryDate(Instant.now().plusMillis(refreshExpTime))
				.revoked(false)
				.build();

		return refreshTokenRepository.save(refreshToken);
	}

	@Override
	@Transactional
	public TokenRefreshResponseDTO refreshAccessToken(TokenRefreshRequestDTO request) {
		RefreshToken token = validateAndGetToken(request.getRefreshToken());
		User user = token.getUser();

		if (!user.isActive()) {
			throw new BadCredentialsException("Your account has been deactivated. Please contact support.");
		}

		return rotateAndGenerateTokens(token, user);
	}

	@Override
	@Transactional
	public TokenRefreshResponseDTO refreshAdminAccessToken(TokenRefreshRequestDTO request) {
		RefreshToken token = validateAndGetToken(request.getRefreshToken());
		User user = token.getUser();

		if (user.getUserRole() != UserRole.ROLE_ADMIN) {
			throw new BadCredentialsException("Unauthorized admin refresh attempt.");
		}

		if (!user.isActive()) {
			throw new BadCredentialsException("Your account has been deactivated.");
		}

		return rotateAndGenerateTokens(token, user);
	}

	@Override
	@Transactional
	public void revokeRefreshToken(String token) {
		refreshTokenRepository.deleteByToken(token);
		log.info("Revoked refresh token successfully.");
	}

	@Override
	@Transactional
	public void revokeAllUserTokens(Long userId) {
		userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
		log.info("Revoked all refresh tokens for user id: {}", userId);
	}

	private RefreshToken validateAndGetToken(String tokenString) {
		RefreshToken token = refreshTokenRepository.findByToken(tokenString)
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token. Please sign in again."));

		if (token.isRevoked()) {
			refreshTokenRepository.delete(token);
			throw new BadCredentialsException("Refresh token was revoked. Please sign in again.");
		}

		if (token.getExpiryDate().isBefore(Instant.now())) {
			refreshTokenRepository.delete(token);
			throw new BadCredentialsException("Refresh token was expired. Please sign in again.");
		}

		return token;
	}

	private TokenRefreshResponseDTO rotateAndGenerateTokens(RefreshToken oldToken, User user) {
		// Delete old token for rotation
		refreshTokenRepository.delete(oldToken);

		// Create rotated refresh token
		String newRefreshTokenString = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
		RefreshToken newRefreshToken = RefreshToken.builder()
				.user(user)
				.token(newRefreshTokenString)
				.expiryDate(Instant.now().plusMillis(refreshExpTime))
				.revoked(false)
				.build();
		refreshTokenRepository.save(newRefreshToken);

		// Issue fresh access token
		String newAccessToken = jwtUtils.generateTokenForUser(user.getId(), user.getEmail(), user.getUserRole().name());

		return TokenRefreshResponseDTO.builder()
				.accessToken(newAccessToken)
				.refreshToken(newRefreshTokenString)
				.tokenType("Bearer")
				.userId(user.getId())
				.userRole(user.getUserRole().name())
				.email(user.getEmail())
				.build();
	}
}
