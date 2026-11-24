package com.mygaadi.payment.dtos;

import java.math.BigDecimal;

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
public class AdminPaymentResponseDTO {
	private PageResponse<PaymentDTO> payments;
	private BigDecimal totalPaidAmount;
	private BigDecimal totalRefundedAmount;
	private long totalCount;
}
