package com.mygaadi.dtos;

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
public class AuthResponse {
	private Long id;
	private String completeName;
	private String email;
	private String userRole;
	private String jwt;
	private String refreshToken;
	private SellerProfileDTO sellerProfile;
	private BuyerProfileDTO buyerProfile;
}
