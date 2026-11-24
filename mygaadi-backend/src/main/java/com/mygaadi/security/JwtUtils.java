package com.mygaadi.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.mygaadi.custom_exceptions.ApiException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtUtils {

	@Value("${jwt.exp.time:7200000}")
	private long expTime;

	@Value("${jwt.secret.key}")
	private String key;

	private final Environment environment;
	private SecretKey secretKey;

	public JwtUtils(Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	public void init() {
		if (key == null || key.length() < 64) {
			throw new ApiException("JWT_SECRET must be at least 64 characters long.");
		}

		boolean isProd = false;
		String[] activeProfiles = environment.getActiveProfiles();
		for (String profile : activeProfiles) {
			if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
				isProd = true;
				break;
			}
		}

		if (key.contains("development_purposes_only") || key.contains("dev_default")) {
			if (isProd) {
				throw new IllegalStateException("FATAL: Dev default JWT_SECRET detected in production profile! Server refusing to start.");
			} else {
				log.warn("SECURITY WARNING: Using default development JWT secret key. Do NOT use in production!");
			}
		}

		this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
		log.info("Initialized HMAC-SHA secret key successfully (length: {} chars)", key.length());
	}

	public String generateJwt(CustomUserDetailsImpl userDetails) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expTime);

		return Jwts.builder()
				.subject(userDetails.getEmail())
				.issuedAt(now)
				.expiration(expiry)
				.claims(Map.of(
						"user_id", userDetails.getUserId(),
						"user_role", userDetails.getRole().name()
				))
				.signWith(secretKey)
				.compact();
	}

	public String generateTokenForUser(Long userId, String email, String userRole) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expTime);

		return Jwts.builder()
				.subject(email)
				.issuedAt(now)
				.expiration(expiry)
				.claims(Map.of(
						"user_id", userId,
						"user_role", userRole
				))
				.signWith(secretKey)
				.compact();
	}

	public Claims verifyJwt(String jwt) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(jwt)
				.getPayload();
	}
}
