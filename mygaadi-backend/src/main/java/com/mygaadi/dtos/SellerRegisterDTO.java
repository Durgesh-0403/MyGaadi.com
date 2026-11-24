package com.mygaadi.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class SellerRegisterDTO {

	@NotBlank(message = "First name is required")
	@Size(min = 2, max = 60, message = "First name must be between 2 and 60 characters")
	private String firstName;

	@NotBlank(message = "Last name is required")
	@Size(min = 2, max = 60, message = "Last name must be between 2 and 60 characters")
	private String lastName;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 6, max = 100, message = "Password must be at least 6 characters")
	private String password;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be a 10-digit number")
	private String phone;

	@NotBlank(message = "Showroom name is required")
	@Size(min = 2, max = 150, message = "Showroom name must be between 2 and 150 characters")
	private String showroomName;

	@NotBlank(message = "City is required")
	private String city;

	@NotBlank(message = "State is required")
	private String state;

	@NotBlank(message = "Showroom address is required")
	private String address;

	private String gstin;
}
