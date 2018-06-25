package com.kumuluz.ee.nodejs.samples.tutorial.java.service.api.v1.mappers;

public class ExceptionResponseObject {
	
	public int status;
	public String message;
	
	public ExceptionResponseObject(int status, String message) {
		this.status = status;
		this.message = message;
	}
}
