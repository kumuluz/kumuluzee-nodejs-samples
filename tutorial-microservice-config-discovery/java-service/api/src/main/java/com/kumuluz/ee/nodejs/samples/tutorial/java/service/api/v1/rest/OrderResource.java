package com.kumuluz.ee.nodejs.samples.tutorial.java.service.api.v1.rest;

import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.models.Order;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.requests.OrderRequest;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.services.OrdersBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
@Path("orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {
	
	@Inject
	private OrdersBean ordersBean;
	
	// get all orders from customer with given id
	@GET
	@Path("customer/{customerId}")
	public Response getOrdersFromCustomer(@PathParam("customerId") long customerId) {
		List<Order> orders = ordersBean.getAllOrdersFromCustomer(customerId);
		return Response.status(Response.Status.OK).entity(orders).build();
	}
	
	// create new order
	@POST
	public Response createOrderForCustomer(OrderRequest newOrder) {
		Order order = new Order();
		order.setCustomerId(newOrder.getCustomerId());
		order.setTitle(newOrder.getTitle());
		order.setDescription(newOrder.getTitle());
		
		ordersBean.createOrder(order);
		
		return Response.status(Response.Status.CREATED).entity(order).build();
	}
}
