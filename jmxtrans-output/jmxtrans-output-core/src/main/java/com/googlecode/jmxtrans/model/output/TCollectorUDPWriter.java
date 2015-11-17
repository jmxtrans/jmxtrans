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
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;


/**
 * {@link com.googlecode.jmxtrans.model.OutputWriter} for the <a href="https://github.com/OpenTSDB/tcollector/blob/master/collectors/0/udp_bridge.py">TCollector udp_bridge</a>.
 * Largely based on StatsDWriter and OpenTSDBWriter
 *
 * @author Kieren Hynd
 * @author Arthur Naseef
 */
public class TCollectorUDPWriter extends OpenTSDBGenericWriter {
	private static final Logger log = LoggerFactory.getLogger(TCollectorUDPWriter.class);

	protected SocketAddress address;
	protected DatagramSocket dgSocket;

	@JsonCreator
	public TCollectorUDPWriter(
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
		super(typeNames, booleanAsNumber, debugEnabled, host, port, tags, tagName, mergeTypeNamesTags, metricNamingExpression,
				addHostnameTag, settings);
	}

	/**
	 * Do not add the hostname tag "host" with the name of the host by default since tcollector normally adds the
	 * hostname.
	 */
	@Override
	protected boolean getAddHostnameTagDefault() {
		return false;
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
		for (Result result : results) {
			for (String resultString : resultParser(result)) {
				if (isDebugEnabled())
				log.debug("TCollectorUDP Message: {}", resultString);
				this.sendOutput(resultString);
			}
		}
		this.finishOutput();
	}

	/**
	 * Setup at start of the writer.
	 */
	@Override
	public void prepareSender() throws LifecycleException {

		if (host == null || port == null) {
			throw new LifecycleException("Host and port for " + this.getClass().getSimpleName() + " output can't be null");
		}

		try {
			this.dgSocket = new DatagramSocket();
			this.address = new InetSocketAddress(host, port);
		} catch (SocketException sockExc) {
			log.error("Failed to create a datagram socket", sockExc);
			throw new LifecycleException(sockExc);
		}
	}

	/**
	 * Send a single metric to TCollector.
	 *
	 * @param metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
	 *                   "put" keyword expected by OpenTSDB and the trailing newline character.
	 */
	@Override
	protected void sendOutput(String metricLine) throws IOException {
		DatagramPacket packet;
		byte[] data;

		data = metricLine.getBytes("UTF-8");
		packet = new DatagramPacket(data, 0, data.length, this.address);

		this.dgSocket.send(packet);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final ImmutableList.Builder<String> typeNames = ImmutableList.builder();
		private boolean booleanAsNumber;
		private Boolean debugEnabled;
		private String host;
		private Integer port;
		private final ImmutableMap.Builder<String, String> tags = ImmutableMap.builder();
		private String tagName;
		private Boolean mergeTypeNamesTags;
		private String metricNamingExpression;
		private Boolean addHostnameTag;

		private Builder() {}

		public Builder addTypeNames(List<String> typeNames) {
			this.typeNames.addAll(typeNames);
			return this;
		}

		public Builder addTypeName(String typeName) {
			typeNames.add(typeName);
			return this;
		}

		public Builder setBooleanAsNumber(boolean booleanAsNumber) {
			this.booleanAsNumber = booleanAsNumber;
			return this;
		}

		public Builder setDebugEnabled(boolean debugEnabled) {
			this.debugEnabled = debugEnabled;
			return this;
		}

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public Builder setPort(int port) {
			this.port  = port;
			return this;
		}

		public Builder addTag(String key, String value) {
			this.tags.put(key, value);
			return this;
		}

		public Builder addTags(Map<String, String> tags) {
			this.tags.putAll(tags);
			return this;
		}

		public Builder setTagName(String tagName) {
			this.tagName = tagName;
			return this;
		}

		public Builder setMergeTypeNamesTags(Boolean mergeTypeNamesTags) {
			this.mergeTypeNamesTags = mergeTypeNamesTags;
			return this;
		}

		public Builder setMetricNamingExpression(String metricNamingExpression) {
			this.metricNamingExpression = metricNamingExpression;
			return this;
		}

		public Builder setAddHostnameTag(Boolean addHostnameTag) {
			this.addHostnameTag = addHostnameTag;
			return this;
		}

		public TCollectorUDPWriter build() throws LifecycleException, UnknownHostException {
			return new TCollectorUDPWriter(
					typeNames.build(),
					booleanAsNumber,
					debugEnabled,
					host,
					port,
					tags.build(),
					tagName,
					mergeTypeNamesTags,
					metricNamingExpression,
					addHostnameTag,
					null
			);
		}
	}



}
