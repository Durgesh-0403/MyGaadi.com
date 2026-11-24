package com.mygaadi.entities;

import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_car", columnNames = {"user_id", "car_id"})
})
@AttributeOverride(name = "id", column = @Column(name = "wishlist_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "car"})
public class WishlistItem extends BaseClass {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "car_id", nullable = false)
	private Car car;

	@Column(name = "is_deleted", nullable = false)
	@Builder.Default
	private boolean deleted = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}
