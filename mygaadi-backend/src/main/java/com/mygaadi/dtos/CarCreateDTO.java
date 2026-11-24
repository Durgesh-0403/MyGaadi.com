package com.mygaadi.dtos;

import java.math.BigDecimal;

import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.Transmission;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CarCreateDTO {

	@NotBlank(message = "Title is required")
	@Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
	private String title;

	@NotBlank(message = "Brand is required")
	private String brand;

	@NotBlank(message = "Model is required")
	private String model;

	@NotBlank(message = "Variant is required")
	private String variant;

	@NotNull(message = "Manufacture year is required")
	@Min(value = 1990, message = "Manufacture year must be 1990 or later")
	@Max(value = 2026, message = "Manufacture year cannot be in the future")
	private Integer manufactureYear;

	@NotNull(message = "Price is required")
	@DecimalMin(value = "10000.00", message = "Minimum car price is 10,000 INR")
	private BigDecimal price;

	@NotNull(message = "Kilometers driven is required")
	@Min(value = 0, message = "Kilometers driven cannot be negative")
	private Integer kmDriven;

	@NotNull(message = "Fuel type is required")
	private FuelType fuelType;

	@NotNull(message = "Transmission is required")
	private Transmission transmission;

	@NotNull(message = "Body type is required")
	private BodyType bodyType;

	@NotNull(message = "Owner count is required")
	@Min(value = 1, message = "Owner count must be at least 1")
	@Max(value = 10, message = "Owner count cannot exceed 10")
	private Integer ownerCount;

	@NotBlank(message = "Color is required")
	private String color;

	@NotBlank(message = "City is required")
	private String city;

	@NotBlank(message = "State is required")
	private String state;

	private String description;
}
