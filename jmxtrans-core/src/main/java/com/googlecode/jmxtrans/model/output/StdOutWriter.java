package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

import java.util.Map;

/**
 * Basic filter good for testing that just outputs the Result objects using
 * System.out.
 * 
 * @author jon
 */
public class StdOutWriter extends BaseOutputWriter {

	@JsonCreator
	public StdOutWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
	}

	/**
	 * nothing to validate
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		for (Result r : results) {
			System.out.println(r);
		}
	}
}
