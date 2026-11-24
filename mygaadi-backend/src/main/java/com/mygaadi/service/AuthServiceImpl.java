package com.mygaadi.service;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.custom_exceptions.ConflictException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.AdminProfileDTO;
import com.mygaadi.dtos.AuthRequest;
import com.mygaadi.dtos.AuthResponse;
import com.mygaadi.dtos.BuyerProfileDTO;
import com.mygaadi.dtos.BuyerRegisterDTO;
import com.mygaadi.dtos.SellerProfileDTO;
import com.mygaadi.dtos.SellerRegisterDTO;
import com.mygaadi.dtos.UserDTO;
import com.mygaadi.entities.BuyerProfile;
import com.mygaadi.entities.SellerProfile;
import com.mygaadi.entities.User;
import com.mygaadi.entities.UserRole;
import com.mygaadi.repository.AdminProfileRepository;
import com.mygaadi.repository.BuyerProfileRepository;
import com.mygaadi.repository.SellerProfileRepository;
import com.mygaadi.repository.UserRepository;
import com.mygaadi.security.CustomUserDetailsImpl;
import com.mygaadi.security.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final SellerProfileRepository sellerProfileRepository;
	private final BuyerProfileRepository buyerProfileRepository;
	private final AdminProfileRepository adminProfileRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final ModelMapper modelMapper;
	private final RefreshTokenService refreshTokenService;

	@Override
	@Transactional
	public AuthResponse registerBuyer(BuyerRegisterDTO dto) {
		validateUniqueUser(dto.getEmail(), dto.getPhone());

		User user = User.builder()
				.firstName(dto.getFirstName().trim())
				.lastName(dto.getLastName().trim())
				.email(dto.getEmail().trim().toLowerCase())
				.password(passwordEncoder.encode(dto.getPassword()))
				.phone(dto.getPhone().trim())
				.userRole(UserRole.ROLE_BUYER)
				.active(true)
				.build();

		BuyerProfile buyerProfile = BuyerProfile.builder()
				.user(user)
				.build();
		user.setBuyerProfile(buyerProfile);

		User savedUser = userRepository.save(user);
		BuyerProfile savedBuyerProfile = savedUser.getBuyerProfile() != null ? savedUser.getBuyerProfile() : buyerProfile;

		log.info("Registered new buyer with email: {}", savedUser.getEmail());

		CustomUserDetailsImpl userDetails = new CustomUserDetailsImpl(savedUser);
		String token = jwtUtils.generateJwt(userDetails);
		String refreshToken = refreshTokenService.createRefreshToken(savedUser.getId()).getToken();

		return AuthResponse.builder()
				.id(savedUser.getId())
				.completeName(savedUser.getCompleteName())
				.email(savedUser.getEmail())
				.userRole(savedUser.getUserRole().name())
				.jwt(token)
				.refreshToken(refreshToken)
				.buyerProfile(modelMapper.map(savedBuyerProfile, BuyerProfileDTO.class))
				.build();
	}

	@Override
	@Transactional
	public AuthResponse registerSeller(SellerRegisterDTO dto) {
		validateUniqueUser(dto.getEmail(), dto.getPhone());

		User user = User.builder()
				.firstName(dto.getFirstName().trim())
				.lastName(dto.getLastName().trim())
				.email(dto.getEmail().trim().toLowerCase())
				.password(passwordEncoder.encode(dto.getPassword()))
				.phone(dto.getPhone().trim())
				.userRole(UserRole.ROLE_SELLER)
				.active(true)
				.build();

		SellerProfile profile = SellerProfile.builder()
				.user(user)
				.showroomName(dto.getShowroomName().trim())
				.city(dto.getCity().trim())
				.state(dto.getState().trim())
				.address(dto.getAddress().trim())
				.gstin(dto.getGstin() != null && !dto.getGstin().trim().isEmpty() ? dto.getGstin().trim() : null)
				.verified(false)
				.kycStatus("NOT_SUBMITTED")
				.build();

		user.setSellerProfile(profile);

		User savedUser = userRepository.save(user);
		SellerProfile savedProfile = savedUser.getSellerProfile() != null ? savedUser.getSellerProfile() : profile;

		log.info("Registered new seller with showroom {} for email: {}", savedProfile.getShowroomName(), savedUser.getEmail());

		CustomUserDetailsImpl userDetails = new CustomUserDetailsImpl(savedUser);
		String token = jwtUtils.generateJwt(userDetails);
		String refreshToken = refreshTokenService.createRefreshToken(savedUser.getId()).getToken();

		SellerProfileDTO profileDTO = modelMapper.map(savedProfile, SellerProfileDTO.class);

		return AuthResponse.builder()
				.id(savedUser.getId())
				.completeName(savedUser.getCompleteName())
				.email(savedUser.getEmail())
				.userRole(savedUser.getUserRole().name())
				.jwt(token)
				.refreshToken(refreshToken)
				.sellerProfile(profileDTO)
				.build();
	}

	@Override
	@Transactional
	public AuthResponse login(AuthRequest request) {
		String email = request.getEmail().trim().toLowerCase();

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		// Admins are strictly prohibited from using the consumer login endpoint
		if (user.getUserRole() == UserRole.ROLE_ADMIN) {
			log.warn("Blocked ADMIN login attempt via public consumer login endpoint: {}", email);
			throw new BadCredentialsException("Invalid email or password");
		}

		if (!user.isActive()) {
			log.warn("Login attempt for deactivated user: {}", email);
			throw new BadCredentialsException("Account is blocked or deactivated");
		}

		try {
			Authentication auth = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.getPassword())
			);
			CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) auth.getPrincipal();
			String token = jwtUtils.generateJwt(userDetails);
			String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUserId()).getToken();

			AuthResponse.AuthResponseBuilder respBuilder = AuthResponse.builder()
					.id(userDetails.getUserId())
					.completeName(userDetails.getCompleteName())
					.email(user.getEmail())
					.userRole(userDetails.getRole().name())
					.jwt(token)
					.refreshToken(refreshToken);

			if (user.getSellerProfile() != null) {
				respBuilder.sellerProfile(modelMapper.map(user.getSellerProfile(), SellerProfileDTO.class));
			}
			if (user.getBuyerProfile() != null) {
				respBuilder.buyerProfile(modelMapper.map(user.getBuyerProfile(), BuyerProfileDTO.class));
			}

			return respBuilder.build();
		} catch (BadCredentialsException e) {
			log.warn("Failed login attempt for email: {}", email);
			throw new BadCredentialsException("Invalid email or password");
		}
	}

	@Override
	@Transactional
	public AuthResponse adminLogin(AuthRequest request) {
		String email = request.getEmail().trim().toLowerCase();

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		// Only ROLE_ADMIN may authenticate through the admin login endpoint
		if (user.getUserRole() != UserRole.ROLE_ADMIN) {
			log.warn("Unauthorized non-admin login attempt at admin portal for user: {}", email);
			throw new BadCredentialsException("Invalid email or password");
		}

		if (!user.isActive()) {
			log.warn("Admin account is deactivated: {}", email);
			throw new BadCredentialsException("Account is blocked or deactivated");
		}

		try {
			Authentication auth = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.getPassword())
			);
			CustomUserDetailsImpl userDetails = (CustomUserDetailsImpl) auth.getPrincipal();
			String token = jwtUtils.generateJwt(userDetails);
			String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUserId()).getToken();

			AuthResponse.AuthResponseBuilder respBuilder = AuthResponse.builder()
					.id(userDetails.getUserId())
					.completeName(userDetails.getCompleteName())
					.email(user.getEmail())
					.userRole(userDetails.getRole().name())
					.jwt(token)
					.refreshToken(refreshToken);

			if (user.getSellerProfile() != null) {
				respBuilder.sellerProfile(modelMapper.map(user.getSellerProfile(), SellerProfileDTO.class));
			}
			if (user.getBuyerProfile() != null) {
				respBuilder.buyerProfile(modelMapper.map(user.getBuyerProfile(), BuyerProfileDTO.class));
			}

			return respBuilder.build();
		} catch (BadCredentialsException e) {
			log.warn("Failed admin login attempt for email: {}", email);
			throw new BadCredentialsException("Invalid email or password");
		}
	}

	@Override
	public UserDTO getCurrentUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		UserDTO dto = modelMapper.map(user, UserDTO.class);
		dto.setCompleteName(user.getCompleteName());

		if (user.getSellerProfile() != null) {
			SellerProfileDTO profileDTO = modelMapper.map(user.getSellerProfile(), SellerProfileDTO.class);
			dto.setSellerProfile(profileDTO);
		}
		if (user.getBuyerProfile() != null) {
			BuyerProfileDTO buyerDTO = modelMapper.map(user.getBuyerProfile(), BuyerProfileDTO.class);
			dto.setBuyerProfile(buyerDTO);
		}
		if (user.getAdminProfile() != null) {
			AdminProfileDTO adminDTO = modelMapper.map(user.getAdminProfile(), AdminProfileDTO.class);
			dto.setAdminProfile(adminDTO);
		}
		return dto;
	}

	private void validateUniqueUser(String email, String phone) {
		if (userRepository.existsByEmail(email.trim().toLowerCase())) {
			throw new ConflictException("Email already registered: " + email);
		}
		if (userRepository.existsByPhone(phone.trim())) {
			throw new ConflictException("Phone number already registered: " + phone);
		}
	}
}
