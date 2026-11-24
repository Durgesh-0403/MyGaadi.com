package com.mygaadi.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mygaadi.dtos.ApiResponse;
import com.mygaadi.dtos.JwtDTO;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomJwtVerificationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String headerValue = request.getHeader("Authorization");

		if (headerValue != null && headerValue.startsWith("Bearer ")) {
			String jwt = headerValue.substring(7).trim();

			try {
				Claims claims = jwtUtils.verifyJwt(jwt);

				Long userId = claims.get("user_id", Long.class);
				String userRole = claims.get("user_role", String.class);
				String email = claims.getSubject();

				JwtDTO jwtDTO = new JwtDTO(userId, userRole, email);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						jwtDTO,
						null,
						List.of(new SimpleGrantedAuthority(userRole))
				);

				SecurityContextHolder.getContext().setAuthentication(authentication);

			} catch (Exception e) {
				log.warn("JWT verification failed for request {}: {}", request.getRequestURI(), e.getMessage());
				SecurityContextHolder.clearContext();

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				ApiResponse apiResponse = new ApiResponse("FAILED", "Invalid or expired JWT token");
				response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
}
