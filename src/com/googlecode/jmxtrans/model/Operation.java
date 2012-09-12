package com.googlecode.jmxtrans.model;

import java.util.Arrays;
import java.util.List;


public class Operation {
	private String operation;
	private List<String> parameters;
	
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public List<String> getParameters() {
		return parameters;
	}
	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

}