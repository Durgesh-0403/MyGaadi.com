package com.mygaadi.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class FeignClientConfig {

	@Value("${internal.api.key}")
	private String internalApiKey;

	@Bean
	public RequestInterceptor requestInterceptor() {
		return template -> {
			template.header("X-Internal-Api-Key", internalApiKey);
		};
	}
}
