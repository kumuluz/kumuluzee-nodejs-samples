package com.kumuluz.ee.nodejs.samples.tutorial.java.service.services;

import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.exceptions.JavaServiceException;
import com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.models.Order;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class OrdersBean {
	
	@PersistenceContext(unitName = "db-jpa-unit")
	private EntityManager entityManager;
	
	public List<Order> getAllOrdersFromCustomer(long customerId) {
		Query query = entityManager.createNamedQuery("Order.findAllByCustomer");
		query.setParameter("customer_id", customerId);
		return query.getResultList();
	}
	
	public Order getOrderById(long orderId) {
		Order order = entityManager.find(Order.class, orderId);
		if (order == null) {
			throw new JavaServiceException("Order not found!", 404);
		}
		return order;
	}
	
	@Transactional
	public void createOrder(Order order) {
		entityManager.persist(order);
	}
}
