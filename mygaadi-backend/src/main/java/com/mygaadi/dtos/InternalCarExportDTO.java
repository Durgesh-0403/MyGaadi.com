package com.mygaadi.dtos;

import java.math.BigDecimal;

import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.Transmission;

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
public class InternalCarExportDTO {
	private Long id;
	private String title;
	private String brand;
	private String model;
	private String variant;
	private Integer manufactureYear;
	private BigDecimal price;
	private Integer kmDriven;
	private FuelType fuelType;
	private Transmission transmission;
	private BodyType bodyType;
	private Integer ownerCount;
	private String color;
	private String city;
	private String state;
	private String description;
	private String status;
	private String approvalStatus;
	private String primaryImageUrl;
}
