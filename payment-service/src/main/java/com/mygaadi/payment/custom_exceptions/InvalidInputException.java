package com.mygaadi.payment.custom_exceptions;

public class InvalidInputException extends RuntimeException {
	public InvalidInputException(String message) {
		super(message);
	}
}
