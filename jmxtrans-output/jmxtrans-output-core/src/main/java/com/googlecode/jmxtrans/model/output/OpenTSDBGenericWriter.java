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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.output.support.opentsdb.OpenTSDBMessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Originally written by Balazs Kossovics <bko@witbe.net>.  Common base class for OpenTSDBWriter and TCollectorWriter.
 * Date: 4/4/13
 * Time: 6:00 PM
 * <p/>
 * Updates by Arthur Naseef
 */
public abstract class OpenTSDBGenericWriter extends BaseOutputWriter {
	public static final boolean DEFAULT_MERGE_TYPE_NAMES_TAGS = true;

	private static final Logger log = LoggerFactory.getLogger(OpenTSDBGenericWriter.class);

	protected final String host;
	protected final Integer port;

	protected final OpenTSDBMessageFormatter messageFormatter;

	@JsonCreator
	public OpenTSDBGenericWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("tags") Map<String, String> tags,
			@JsonProperty("tagName") String tagName,
			@JsonProperty("mergeTypeNamesTags") Boolean mergeTypeNamesTags,
			@JsonProperty("metricNamingExpression") String metricNamingExpression,
			@JsonProperty("addHostnameTag") Boolean addHostnameTag,
			@JsonProperty("settings") Map<String, Object> settings) throws LifecycleException, UnknownHostException {
		super(typeNames, booleanAsNumber, debugEnabled, settings);

		this.host = MoreObjects.firstNonNull(host, (String) getSettings().get(HOST));
		this.port = MoreObjects.firstNonNull(port, Settings.getIntegerSetting(getSettings(), PORT, null));

		if (metricNamingExpression == null) {
			metricNamingExpression = Settings.getStringSetting(this.getSettings(), "metricNamingExpression", null);
		}

		addHostnameTag = firstNonNull(
				addHostnameTag,
				Settings.getBooleanSetting(this.getSettings(), "addHostnameTag", this.getAddHostnameTagDefault()),
				getAddHostnameTagDefault());

		messageFormatter = new OpenTSDBMessageFormatter(typeNames, ImmutableMap.copyOf(
				firstNonNull(
						tags,
						(Map<String, String>) getSettings().get("tags"),
						ImmutableMap.<String, String>of())),
				firstNonNull(tagName, (String) getSettings().get("tagName"), "type"),
				metricNamingExpression,
				MoreObjects.firstNonNull(
						mergeTypeNamesTags,
						Settings.getBooleanSetting(this.getSettings(), "mergeTypeNamesTags", DEFAULT_MERGE_TYPE_NAMES_TAGS)),
				addHostnameTag ? InetAddress.getLocalHost().getHostName() : null);
	}

	/**
	 * Prepare for sending metrics, if needed.  For use by subclasses.
	 */
	protected void prepareSender() throws LifecycleException {
	}

	/**
	 * Shutdown the sender, if needed.  For use by subclasses.
	 */
	protected void shutdownSender() throws LifecycleException {
	}

	/**
	 * Prepare a batch of results output, if needed.  For use by subclasses.
	 */
	protected void startOutput() throws IOException {
	}

	/**
	 * Subclass responsibility: specify the default value for the "addHostnameTag" setting.
	 */
	protected abstract boolean getAddHostnameTagDefault();

	/**
	 * Subcall responsibility: method to perform the actual output for the given metric line.  Every subclass
	 * <b>must</b> implement this method.
	 *
	 * @param metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
	 *                   "put" keyword expected by OpenTSDB and the trailing newline character.
	 */
	protected abstract void sendOutput(String metricLine) throws IOException;

	/**
	 * Complete a batch of results output, if needed.  For use by subclasses.
	 */
	protected void finishOutput() throws IOException {
	}

	/**
	 * Write the results of the query.
	 *
	 * @param server
	 * @param query   - the query and its results.
	 * @param results
	 */
	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		this.startOutput();
		for (String formattedResult : messageFormatter.formatResults(results)) {
				log.debug("Sending result: {}", formattedResult);
				this.sendOutput(formattedResult);
		}
		this.finishOutput();
	}

	/**
	 * Validation per query, after the writer has been start()ed
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	/**
	 * Start the output writer.  At this time, the settings are read from the configuration file and saved for later
	 * use.
	 */
	@Override
	public void start() throws LifecycleException {
		this.prepareSender();
	}

	@Override
	public void stop() throws LifecycleException {
		this.shutdownSender();
	}

}
