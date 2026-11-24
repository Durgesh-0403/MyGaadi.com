package com.mygaadi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

	private final CustomJwtVerificationFilter jwtFilter;
	private final InternalApiKeyFilter internalApiKeyFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(request -> request
				// Preflight OPTIONS requests
				.requestMatchers(HttpMethod.OPTIONS).permitAll()

				// Public Endpoints
				.requestMatchers(
						"/v3/api-docs/**",
						"/swagger-ui/**",
						"/swagger-ui.html",
						"/actuator/health",
						"/actuator/info",
						"/auth/login",
						"/auth/admin/login",
						"/auth/register/**",
						"/auth/refresh",
						"/auth/admin/refresh",
						"/auth/logout",
						"/cars",
						"/cars/**",
						"/uploads/**",
						"/internal/**"
				).permitAll()

				// Buyer Endpoints
				.requestMatchers("/buyer/**").hasRole("BUYER")

				// Seller Endpoints
				.requestMatchers("/seller/**").hasRole("SELLER")

				// Admin Endpoints
				.requestMatchers("/admin/**").hasRole("ADMIN")

				// Authenticated Current User
				.requestMatchers("/auth/me").authenticated()

				.anyRequest().authenticated()
			);

		http.addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
