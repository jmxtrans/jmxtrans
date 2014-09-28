package com.googlecode.jmxtrans.model;

public class ValidationException extends Exception {

	private Query query;

	public ValidationException(String msg, Query query) {
		super(msg);
		this.query = query;
	}

	public Query getQuery() {
		return query;
	}
}
