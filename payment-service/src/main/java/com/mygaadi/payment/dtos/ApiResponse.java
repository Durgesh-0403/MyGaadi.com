package com.mygaadi.payment.dtos;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ApiResponse {
	private String status;
	private LocalDateTime timeStamp;
	private String message;

	public ApiResponse(String status, String message) {
		this.status = status;
		this.message = message;
		this.timeStamp = LocalDateTime.now();
	}
}
