package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

/**
 * Basic filter good for testing that just outputs the Result objects using
 * System.out.
 * 
 * @author jon
 */
public class StdOutWriter extends BaseOutputWriter {

	public StdOutWriter() {
	}

	/**
	 * nothing to validate
	 */
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		for (Result r : results) {
			System.out.println(r);
		}
	}
}
