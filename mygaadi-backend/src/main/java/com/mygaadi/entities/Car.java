package com.mygaadi.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "cars", indexes = {
		@Index(name = "idx_car_brand", columnList = "brand"),
		@Index(name = "idx_car_price", columnList = "price"),
		@Index(name = "idx_car_city", columnList = "city"),
		@Index(name = "idx_car_year", columnList = "manufacture_year"),
		@Index(name = "idx_car_appr_stat_del", columnList = "approval_status, status, is_deleted")
})
@AttributeOverride(name = "id", column = @Column(name = "car_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"seller", "images"})
public class Car extends BaseClass {

	@Column(name = "title", length = 150, nullable = false)
	private String title;

	@Column(name = "brand", length = 60, nullable = false)
	private String brand;

	@Column(name = "model", length = 60, nullable = false)
	private String model;

	@Column(name = "variant", length = 60, nullable = false)
	private String variant;

	@Column(name = "manufacture_year", nullable = false)
	private Integer manufactureYear;

	@Column(name = "price", precision = 12, scale = 2, nullable = false)
	private BigDecimal price;

	@Column(name = "km_driven", nullable = false)
	private Integer kmDriven;

	@Enumerated(EnumType.STRING)
	@Column(name = "fuel_type", length = 30, nullable = false)
	private FuelType fuelType;

	@Enumerated(EnumType.STRING)
	@Column(name = "transmission", length = 30, nullable = false)
	private Transmission transmission;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_type", length = 30, nullable = false)
	private BodyType bodyType;

	@Column(name = "owner_count", nullable = false)
	@Builder.Default
	private Integer ownerCount = 1;

	@Column(name = "color", length = 40, nullable = false)
	private String color;

	@Column(name = "city", length = 80, nullable = false)
	private String city;

	@Column(name = "state", length = 80, nullable = false)
	private String state;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	@Builder.Default
	private CarStatus status = CarStatus.AVAILABLE;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", length = 30, nullable = false)
	@Builder.Default
	private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

	@Column(name = "rejection_reason", length = 255)
	private String rejectionReason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	@OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder ASC")
	@Builder.Default
	private List<CarImage> images = new ArrayList<>();

	@Version
	@Column(name = "version")
	private Long version;

	@Column(name = "is_deleted", nullable = false)
	@Builder.Default
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public void addImage(CarImage image) {
		images.add(image);
		image.setCar(this);
	}

	public void removeImage(CarImage image) {
		images.remove(image);
		image.setCar(null);
	}

	public String getPrimaryImageUrl() {
		if (images == null || images.isEmpty()) {
			return null;
		}
		for (CarImage img : images) {
			if (img.isPrimary()) {
				return img.getUrl();
			}
		}
		return images.get(0).getUrl();
	}
}
