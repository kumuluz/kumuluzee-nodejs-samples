package com.kumuluz.ee.nodejs.samples.tutorial.java.service.persistence.models;

import javax.persistence.*;

@Entity
@Table(name = "orders")
@NamedQueries({
	@NamedQuery(name = "Order.findAllByCustomer", query = "SELECT o FROM Order o WHERE o.customerId = :customer_id")
})
public class Order {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(name = "title")
	private String title;
	
	@Column(name = "description")
	private String description;
	
	@Column(name = "customer_id")
	private long customerId;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public long getCustomerId() {
		return customerId;
	}
	
	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}
	
}
