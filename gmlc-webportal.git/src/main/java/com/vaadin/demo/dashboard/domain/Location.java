package com.vaadin.demo.dashboard.domain;

import java.util.Date;

public final class Location {
	private int id;
	private Date dateTime;
	private String msisdn;
	private String cellId;
	private String serviceId;
	
	private String vlrId;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	
	
	public Date getDateTime() {
		return dateTime;
	}
	
	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}
	
	
	
	public String getMsisdn() {
		return msisdn;
	}
	
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	
	public String getCellId() {
		return cellId;
	}
	
	public void setCellId(String cellId) {
		this.cellId = cellId;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
}
