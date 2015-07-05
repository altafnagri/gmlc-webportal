package com.vaadin.demo.dashboard.domain;

import java.math.BigDecimal;
import java.util.Date;

public class Detailed {
	private Date time;
	private String serviceID;
	private long totalRequests;
	private BigDecimal successfulRequests;
	private BigDecimal failedRequests;
	
	public Date getTime() {
        return time;
    }

    public void setTime(final Date time) {
        this.time = time;
    }
	
    public String getServiceID() {
		return serviceID;
	}
	
	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}
	
	public long getTotalRequests() {
		return totalRequests;
	}
	
	public void setTotalRequests(long totalRequests) {
		this.totalRequests = totalRequests;
	}
	
	public BigDecimal getSuccessfulRequests() {
		return successfulRequests;
	}
	
	public void setSuccessfulRequests(BigDecimal successfulRequests) {
		this.successfulRequests = successfulRequests;
	}
	
	public BigDecimal getFailedRequests() {
		return failedRequests;
	}
	
	public void setFailedRequest(BigDecimal failedRequests) {
		this.failedRequests = failedRequests;
	}
}
