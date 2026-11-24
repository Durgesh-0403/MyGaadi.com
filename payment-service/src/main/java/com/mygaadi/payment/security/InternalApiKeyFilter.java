package com.mygaadi.payment.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mygaadi.payment.dtos.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

	@Value("${internal.api.key}")
	private String internalApiKey;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String uri = request.getRequestURI();

		if (uri.startsWith("/internal")) {
			String providedKey = request.getHeader("X-Internal-Api-Key");

			if (providedKey == null || !isValidKey(providedKey)) {
				log.warn("Unauthorized attempt to access internal payment endpoint: {}", uri);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				ApiResponse apiResponse = new ApiResponse("FAILED", "Forbidden: Invalid internal API key");
				response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isValidKey(String providedKey) {
		if (internalApiKey == null || providedKey == null) {
			return false;
		}
		byte[] a = internalApiKey.getBytes(StandardCharsets.UTF_8);
		byte[] b = providedKey.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(a, b);
	}
}
