package com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.exceptions;

public class JavaServiceException extends RuntimeException {
	
	public int status;
	
	public JavaServiceException(String message, int status) {
		super(message);
		this.status = status;
	}
}
