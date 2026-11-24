package com.mygaadi.dtos;

import java.time.LocalDateTime;

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
public class SellerProfileDTO {
	private String showroomName;
	private String city;
	private String state;
	private String address;
	private String gstin;
	private boolean verified;

	private String kycDocumentType;
	private String kycDocumentNumber;
	private String kycDocumentUrl;
	private String kycStatus;
	private String kycRejectionReason;
	private LocalDateTime kycSubmittedAt;
	private LocalDateTime kycVerifiedAt;
}
