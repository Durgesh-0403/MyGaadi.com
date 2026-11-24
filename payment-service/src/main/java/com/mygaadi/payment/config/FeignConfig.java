package com.mygaadi.payment.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableFeignClients(basePackages = "com.mygaadi.payment.client")
@Profile("!test")
public class FeignConfig {
}
