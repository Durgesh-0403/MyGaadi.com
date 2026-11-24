package com.mygaadi.dtos;

import jakarta.validation.constraints.NotBlank;
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
public class SellerKycStatusUpdateDTO {
	@NotBlank(message = "Status is required (VERIFIED or REJECTED)")
	private String status;

	private String rejectionReason;
}
