package com.mygaadi.dtos;

import com.mygaadi.entities.UserRole;

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
public class UserDTO {
	private Long id;
	private String firstName;
	private String lastName;
	private String completeName;
	private String email;
	private String phone;
	private UserRole userRole;
	private boolean active;
	private SellerProfileDTO sellerProfile;
	private BuyerProfileDTO buyerProfile;
	private AdminProfileDTO adminProfile;
}
