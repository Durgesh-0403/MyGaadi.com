package com.mygaadi.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.CarStatus;
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
public class CarDTO {
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
	private CarStatus status;
	private ApprovalStatus approvalStatus;
	private String rejectionReason;
	private Long sellerId;
	private String sellerName;
	private String sellerShowroom;
	private boolean sellerVerified;
	private String primaryImageUrl;
	private List<CarImageDTO> images;
	private Long version;
	private boolean deleted;
	private LocalDate createdOn;
	private LocalDateTime lastUpdated;
}
