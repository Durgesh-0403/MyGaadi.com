package com.mygaadi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		final String securitySchemeName = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("MyGaadi.com Backend API")
						.description("Core Used-Car Marketplace REST APIs with Spring Boot 3.5 & JWT Security")
						.version("1.0.0")
						.license(new License().name("Proprietary").url("https://mygaadi.com")))
				.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
				.components(new Components().addSecuritySchemes(
						securitySchemeName,
						new SecurityScheme()
								.name(securitySchemeName)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
				));
	}
}
