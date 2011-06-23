package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.model.Query;

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
