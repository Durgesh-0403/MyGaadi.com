package com.mygaadi.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "car_images")
@AttributeOverride(name = "id", column = @Column(name = "image_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "car")
public class CarImage extends BaseClass {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "car_id", nullable = false)
	private Car car;

	@Column(name = "file_name", length = 255, nullable = false)
	private String fileName;

	@Column(name = "image_url", length = 500, nullable = false)
	private String url;

	@Column(name = "is_primary", nullable = false)
	@Builder.Default
	private boolean primary = false;

	@Column(name = "image_category", length = 30)
	@Builder.Default
	private String category = "EXTERIOR";

	@Column(name = "display_order", nullable = false)
	@Builder.Default
	private int displayOrder = 0;
}
