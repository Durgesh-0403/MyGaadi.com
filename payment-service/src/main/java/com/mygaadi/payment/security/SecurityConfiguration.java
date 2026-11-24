package com.mygaadi.payment.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
	public SecurityFilterChain paymentFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(request -> request
				// Allow preflights
				.requestMatchers(HttpMethod.OPTIONS).permitAll()

				// Public endpoints (Swagger, actuator, webhook)
				.requestMatchers(
						"/v3/api-docs/**",
						"/swagger-ui/**",
						"/swagger-ui.html",
						"/actuator/health",
						"/actuator/info",
						"/payments/webhook",
						"/internal/**"
				).permitAll()

				// Buyer payment endpoints
				.requestMatchers(
						"/payments/orders",
						"/payments/verify",
						"/payments/failure",
						"/payments/my"
				).hasRole("BUYER")

				// Shared buyer/admin booking check
				.requestMatchers("/payments/booking/**").hasAnyRole("BUYER", "ADMIN")

				// Admin endpoints
				.requestMatchers("/payments/admin/**").hasRole("ADMIN")

				.anyRequest().authenticated()
			);

		http.addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
