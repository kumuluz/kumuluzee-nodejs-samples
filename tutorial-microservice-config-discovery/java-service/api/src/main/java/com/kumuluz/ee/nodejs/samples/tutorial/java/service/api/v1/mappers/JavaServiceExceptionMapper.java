package com.kumuluz.ee.nodejs.samples.tutorial.java.service.api.v1.mappers;

import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.exceptions.JavaServiceException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JavaServiceExceptionMapper implements ExceptionMapper<JavaServiceException> {
	
	@Override
	public Response toResponse(JavaServiceException exception) {
		ExceptionResponseObject response = new ExceptionResponseObject(
			exception.status, exception.getMessage());
		return Response.status(Response.Status.fromStatusCode(exception.status)).entity(response).build();
	}
	
}
