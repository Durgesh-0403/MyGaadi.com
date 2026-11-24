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
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class SellerProfile implements Persistable<Long> {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "showroom_name", length = 150, nullable = false)
	private String showroomName;

	@Column(name = "city", length = 80, nullable = false)
	private String city;

	@Column(name = "state", length = 80, nullable = false)
	private String state;

	@Column(name = "address", length = 255, nullable = false)
	private String address;

	@Column(name = "gstin", length = 30)
	private String gstin;

	@Column(name = "is_verified", nullable = false)
	@Builder.Default
	private boolean verified = false;

	// KYC Verification Fields
	@Column(name = "kyc_document_type", length = 30)
	private String kycDocumentType; // "PAN" or "AADHAAR"

	@Column(name = "kyc_document_number", length = 50)
	private String kycDocumentNumber;

	@Column(name = "kyc_document_url", length = 255)
	private String kycDocumentUrl;

	@Column(name = "kyc_status", length = 30)
	@Builder.Default
	private String kycStatus = "NOT_SUBMITTED"; // "NOT_SUBMITTED", "PENDING", "VERIFIED", "REJECTED"

	@Column(name = "kyc_rejection_reason", length = 255)
	private String kycRejectionReason;

	@Column(name = "kyc_submitted_at")
	private LocalDateTime kycSubmittedAt;

	@Column(name = "kyc_verified_at")
	private LocalDateTime kycVerifiedAt;

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
