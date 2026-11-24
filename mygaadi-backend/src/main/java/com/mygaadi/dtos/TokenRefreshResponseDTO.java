package com.mygaadi.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponseDTO {

	private String accessToken;
	private String refreshToken;
	@Builder.Default
	private String tokenType = "Bearer";
	private Long userId;
	private String userRole;
	private String email;
}
