package com.mygaadi.exception_handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.mygaadi.custom_exceptions.ApiException;
import com.mygaadi.custom_exceptions.ConflictException;
import com.mygaadi.custom_exceptions.InvalidInputException;
import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.ApiResponse;

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

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiResponse> handleConflictException(ConflictException e) {
		log.warn("ConflictException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiResponse("FAILED", e.getMessage()));
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException e) {
		log.warn("Optimistic locking conflict: {}", e.getMessage(), e);
		String message = "A concurrent modification conflict occurred. Please refresh and try again.";
		if (e.getPersistentClassName() != null) {
			if (e.getPersistentClassName().contains("Car") || e.getPersistentClassName().contains("Booking")) {
				message = "This car was just reserved or updated by another user. Please refresh and try again.";
			}
		} else if (e.getMessage() != null && (e.getMessage().contains("Car") || e.getMessage().contains("Booking"))) {
			message = "This car was just reserved or updated by another user. Please refresh and try again.";
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiResponse("FAILED", message));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
		log.warn("Database integrity violation: {}", e.getMessage());
		String msg = "Data integrity violation occurred";
		String raw = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
		if (raw.contains("email") || raw.contains("users.email") || raw.contains("uk_")) {
			msg = "Email address or phone number is already registered. Please sign in or use a different one.";
		} else if (raw.contains("phone")) {
			msg = "Phone number is already registered. Please use a different phone number.";
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiResponse("FAILED", msg));
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

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException e) {
		log.warn("Bad credentials authentication failure");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiResponse("FAILED", "Invalid email or password"));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException e) {
		log.warn("AuthenticationException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiResponse("FAILED", "Authentication failed"));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
		log.warn("AccessDeniedException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiResponse("FAILED", "Access denied: insufficient permissions"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
		log.warn("Max upload size exceeded: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(new ApiResponse("FAILED", "Upload exceeds allowed size limit"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
		List<FieldError> fieldErrors = e.getFieldErrors();
		Map<String, String> errors = fieldErrors.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Validation error",
						(existing, replacement) -> existing + ", " + replacement
				));
		String summary = String.join(", ", errors.values());
		log.warn("Validation error on fields: {}", errors.keySet());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
				"status", "FAILED",
				"message", summary,
				"errors", errors
		));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		log.warn("Method not supported: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(new ApiResponse("FAILED", e.getMessage()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse> handleNoResourceFound(NoResourceFoundException e) {
		log.warn("Resource not found: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse("FAILED", e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse> handleCatchAll(Exception e) {
		log.error("Unhandled server exception occurred", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiResponse("FAILED", "An unexpected error occurred. Please try again later."));
	}
}
