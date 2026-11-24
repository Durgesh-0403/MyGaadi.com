package com.mygaadi.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "admin_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class AdminProfile implements Persistable<Long> {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "department", length = 100)
	@Builder.Default
	private String department = "Platform Administration";

	@Column(name = "employee_code", length = 50)
	@Builder.Default
	private String employeeCode = "ADM-001";

	@Column(name = "admin_level", length = 50)
	@Builder.Default
	private String adminLevel = "SUPER_ADMIN";

	@CreationTimestamp
	@Column(name = "created_on", updatable = false)
	private LocalDateTime createdOn;

	@UpdateTimestamp
	@Column(name = "last_updated")
	private LocalDateTime lastUpdated;

	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public Long getId() {
		return this.userId;
	}

	@Override
	public boolean isNew() {
		return this.isNew || this.createdOn == null;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}
}
