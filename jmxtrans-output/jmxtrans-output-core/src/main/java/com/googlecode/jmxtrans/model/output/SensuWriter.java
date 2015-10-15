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
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://sensuapp.org/docs/0.12/events">Sensu Event Data</a>
 * Format from <a href=https://github.com/SimpleFinance/chef-handler-sensu-event">chef-handler-sensu-event</a>
 * Class structure from LibratoWriter
 * <p/>
 * Settings:
 * <ul>
 * <li>"{@code host}": Sensu client host. Optional, default value: {@value #DEFAULT_SENSU_HOST}</li>
 * <li>"{@code handler}": Sensu handler. Optional, default value: {@value #DEFAULT_SENSU_HANDLER}</li>
 * </ul>
 *
 * @author <a href="mailto:jhmartin@toger.us">Jason Martin</a>
 */
public class SensuWriter extends BaseOutputWriter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final JsonFactory jsonFactory = new JsonFactory();

	public final static String SETTING_HANDLER = "handler";
	public final static String DEFAULT_SENSU_HOST = "localhost";
	public final static String DEFAULT_SENSU_HANDLER = "graphite";

	/**
	 * Sensu HTTP API URL
	 */
	private final String host;
	private final String handler;

	@JsonCreator
	public SensuWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("host") String host,
			@JsonProperty("handler") String handler,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.host = firstNonNull(host, (String) getSettings().get(HOST), DEFAULT_SENSU_HOST);
		this.handler = firstNonNull(handler, (String) getSettings().get(SETTING_HANDLER), DEFAULT_SENSU_HANDLER);
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
		logger.info("Start Sensu writer connected to '{}' with handler {}", host, handler);
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		logger.debug("Export to '{}', metrics {}", host, query);
		writeToSensu(server, query, results);
	}

	private void serialize(Server server, Query query, List<Result> results, OutputStream outputStream) throws IOException {
		JsonGenerator g = jsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		g.writeStringField("name", "jmxtrans");
		g.writeStringField("type", "metric");
		g.writeStringField("handler", handler);

		StringBuilder jsonoutput = new StringBuilder();
		List<String> typeNames = getTypeNames();
		for (Result result : results) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Map.Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {
						Object value = values.getValue();
						jsonoutput.append(KeyUtils.getKeyString(server, query, result, values, typeNames, null)).append(" ")
								.append(value).append(" ")
								.append(TimeUnit.SECONDS.convert(result.getEpoch(), TimeUnit.MILLISECONDS))
								.append(System.getProperty("line.separator"));
					}
				}
			}
		}
		g.writeStringField("output", jsonoutput.toString());
		g.writeEndObject();
		g.flush();
		g.close();
	}

	private void writeToSensu(Server server, Query query, List<Result> results) {
		Socket socketConnection = null;
		try {
			socketConnection = new Socket(host, 3030);
			serialize(server, query, results, socketConnection.getOutputStream());
		} catch (Exception e) {
			logger.warn("Failure to send result to Sensu server '{}'", host, e);
		} finally {
			if (socketConnection != null) {
				try {
					socketConnection.close();
				} catch (IOException e) {
					logger.warn("Exception closing Sensu connection", e);
				}
			}
		}
	}

	public String getHost() {
		return host;
	}

	public String getHandler() {
		return handler;
	}
}
