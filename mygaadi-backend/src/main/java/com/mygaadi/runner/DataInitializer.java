package com.mygaadi.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.entities.AdminProfile;
import com.mygaadi.entities.User;
import com.mygaadi.entities.UserRole;
import com.mygaadi.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		log.info("Starting MyGaadi Data Seeding (Admin Only)...");

		// Seed or Update Super Admin
		User admin = userRepository.findByEmail("admin@mygaadi.com").orElse(null);
		if (admin == null) {
			admin = User.builder()
					.firstName("Super")
					.lastName("Admin")
					.email("admin@mygaadi.com")
					.password(passwordEncoder.encode("admin@1234"))
					.phone("9876543210")
					.userRole(UserRole.ROLE_ADMIN)
					.active(true)
					.build();

			AdminProfile adminProfile = AdminProfile.builder()
					.user(admin)
					.department("Platform Administration")
					.employeeCode("ADM-001")
					.adminLevel("SUPER_ADMIN")
					.build();
			admin.setAdminProfile(adminProfile);
			admin = userRepository.save(admin);
			log.info("Seeded Admin: admin@mygaadi.com (password: admin@1234)");
		} else {
			admin.setPassword(passwordEncoder.encode("admin@1234"));
			admin.setActive(true);
			admin = userRepository.save(admin);
			log.info("Updated Admin: admin@mygaadi.com (password: admin@1234)");
		}

		log.info("Data seeding complete. Only Super Admin seeded.");
	}
}
