package com.mygaadi.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

	@Value("${app.upload.dir:./uploads}")
	private String uploadDir;

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.getConfiguration()
				.setMatchingStrategy(MatchingStrategies.STRICT)
				.setSkipNullEnabled(true);
		return modelMapper;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
		String uploadResourceLocation = "file:" + uploadPath.toString() + "/";

		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadResourceLocation)
				.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
	}
}
