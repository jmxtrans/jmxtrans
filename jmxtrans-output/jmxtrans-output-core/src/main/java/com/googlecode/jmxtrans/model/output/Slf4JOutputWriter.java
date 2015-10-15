/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.googlecode.jmxtrans.model.naming.StringUtils.cleanupStr;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.lang.String.valueOf;

public class Slf4JOutputWriter extends BaseOutputWriter {

	private final Logger logger;

	@JsonCreator
	public Slf4JOutputWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("logger") String logger,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		String loggerName = MoreObjects.firstNonNull(logger, "jmxtrans.output");
		this.logger = LoggerFactory.getLogger(loggerName);
	}

	@VisibleForTesting
	Slf4JOutputWriter(Logger logger) {
		super(ImmutableList.<String>of(),
				true,
				false,
				new HashMap<String, Object>());
		this.logger = logger;
	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		final List<String> typeNames = getTypeNames();

		for (final Result result : results) {
			final Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (final Map.Entry<String, Object> values : resultValues.entrySet()) {
					logValue(server, query, typeNames, result, values);
				}
			}
		}
	}

	private void logValue(Server server, Query query, List<String> typeNames, Result result, Map.Entry<String, Object> values) throws IOException {
		Object value = values.getValue();

		if (value != null && isNumeric(value)) {
			Closer closer = Closer.create();
			try {
				closer.register(MDC.putCloseable("server", computeAlias(server)));
				closer.register(MDC.putCloseable("metric", KeyUtils.getKeyString(server, query, result, values, typeNames, null)));
				closer.register(MDC.putCloseable("value", value.toString()));
				if (result.getKeyAlias() != null) {
					closer.register(MDC.putCloseable("resultAlias", result.getKeyAlias()));
				}
				closer.register(MDC.putCloseable("attributeName", result.getAttributeName()));
				closer.register(MDC.putCloseable("key", values.getKey()));
				closer.register(MDC.putCloseable("epoch", valueOf(result.getEpoch())));

				logger.info("");
			} catch (Throwable t) {
				closer.rethrow(t);
			} finally {
				closer.close();
			}
		}
	}

	private String computeAlias(Server server) {
		if (server.getAlias() != null) return server.getAlias();
		return cleanupStr(server.getHost() + "_" + server.getPort());
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {}

}
