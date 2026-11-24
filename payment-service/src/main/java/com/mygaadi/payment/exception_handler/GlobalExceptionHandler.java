package com.mygaadi.payment.exception_handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mygaadi.payment.custom_exceptions.ApiException;
import com.mygaadi.payment.custom_exceptions.InvalidInputException;
import com.mygaadi.payment.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.payment.dtos.ApiResponse;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
		log.warn("ResourceNotFoundException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse("FAILED", e.getMessage()));
	}

	@ExceptionHandler(InvalidInputException.class)
	public ResponseEntity<ApiResponse> handleInvalidInputException(InvalidInputException e) {
		log.warn("InvalidInputException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiResponse("FAILED", e.getMessage()));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse> handleApiException(ApiException e) {
		log.error("ApiException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiResponse("FAILED", e.getMessage()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
		log.warn("AccessDeniedException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiResponse("FAILED", "Access denied: insufficient permissions"));
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<ApiResponse> handleFeignException(FeignException e) {
		log.error("Downstream backend service communication error: status={}, message={}", e.status(), e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(new ApiResponse("FAILED", "Core backend communication error: " + (e.status() > 0 ? "HTTP " + e.status() : "Service unreachable")));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
		List<FieldError> fieldErrors = e.getFieldErrors();
		Map<String, String> errors = fieldErrors.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Validation error",
						(existing, replacement) -> existing + ", " + replacement
				));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse> handleCatchAll(Exception e) {
		log.error("Unhandled payment service exception occurred", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiResponse("FAILED", "An unexpected payment error occurred. Please try again later."));
	}
}
