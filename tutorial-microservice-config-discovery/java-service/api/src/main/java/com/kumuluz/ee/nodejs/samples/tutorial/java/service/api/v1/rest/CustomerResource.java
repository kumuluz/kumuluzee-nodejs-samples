package com.kumuluz.ee.nodejs.samples.tutorial.java.service.api.v1.rest;

import com.kumuluz.ee.discovery.annotations.DiscoverService;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.exceptions.JavaServiceException;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.models.Order;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.responses.CustomerResponse;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.services.OrdersBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;


@ApplicationScoped
@Path("customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {
	
	@Inject
	private OrdersBean ordersBean;
	
	@Inject
	@DiscoverService(value = "node-service", version = "1.0.0", environment = "dev")
	private Optional<WebTarget> serviceUrl;
	
	// get customer for given order id
	@GET
	@Path("{orderId}")
	public Response getCustomerFromOrder(@PathParam("orderId") long orderId) {
		Order order = ordersBean.getOrderById(orderId);
		
		if (!serviceUrl.isPresent()) {
			throw new JavaServiceException("Service URL not found!", 404);
		}
		
		WebTarget apiUrl = serviceUrl.get().path("v1/customers/" + order.getCustomerId());
		
		Response response = apiUrl.request().get();
		
		if (response.getStatus() == 200) {
			CustomerResponse customerResponse = response.readEntity(CustomerResponse.class);
			return Response.status(Response.Status.OK).entity(customerResponse).build();
		} else {
			throw new JavaServiceException("Service returned error status code: " + response.getStatus(), 500);
		}
		
	}
}
