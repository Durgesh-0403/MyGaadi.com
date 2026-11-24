package com.mygaadi.dtos;

import java.math.BigDecimal;
import java.util.Map;

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
public class AdminDashboardDTO {
	private Map<String, Long> usersByRole;
	private Map<String, Long> carsByApprovalStatus;
	private Map<String, Long> carsByStatus;
	private Map<String, Long> bookingsByStatus;
	private long totalUsers;
	private long totalCars;
	private long totalBookings;
	private BigDecimal totalTokenRevenue;
}
