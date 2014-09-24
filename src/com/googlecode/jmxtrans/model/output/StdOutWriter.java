package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;

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
	public void validateSetup(Query query) throws ValidationException {
	}

	public void doWrite(Query query, ImmutableList<Result> results) throws Exception {
		for (Result r : results) {
			System.out.println(r);
		}
	}
}
