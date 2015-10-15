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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;

/**
 * OpenTSDBWriter which directly sends
 * <p/>
 * Created from sources originally written by Balazs Kossovics <bko@witbe.net>
 */

public class OpenTSDBWriter extends OpenTSDBGenericWriter {
	private static final Logger log = LoggerFactory.getLogger(OpenTSDBWriter.class);

	private GenericKeyedObjectPool<InetSocketAddress, Socket> pool;
	private final InetSocketAddress address;

	@JsonCreator
	public OpenTSDBWriter(
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
		host = resolveProps(host);
		if (host == null) {
			host = (String) getSettings().get(HOST);
		}
		if (host == null) {
			throw new NullPointerException("Host cannot be null.");
		}
		if (port == null) {
			port = Settings.getIntegerSetting(getSettings(), PORT, null);
		}
		if (port == null) {
			throw new NullPointerException("Port cannot be null.");
		}
		this.address = new InetSocketAddress(host, port);

	}

	/**
	 * Add the hostname tag "host" with the name of the host by default since OpenTSDB otherwise does not have this
	 * information.
	 */
	@Override
	protected boolean getAddHostnameTagDefault() {
		return true;
	}

	@Override
	protected void sendOutput(String metricLine) throws IOException {
	}

	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
	Socket socket = null;
	PrintWriter writer = null;

	try {
		socket = pool.borrowObject(address);
		writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

		for (Result result : results) {
			log.debug("Query result: {}", result);
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (String resultString : resultParser(result)) {
					log.debug("OpenTSDB Message: {}", resultString);
					writer.write("put " + resultString + "\n");
				}
			}
		}

	} catch (ConnectException e) {
		log.error("Error while connecting to OpenTSDB");
	} finally {
		if (writer != null && writer.checkError()) {
			log.error("Error writing to OpenTSDB, clearing OpenTSDB socket pool");
			pool.invalidateObject(address, socket);
		} else {
			pool.returnObject(address, socket);
		}
	}
	}

	@Inject
	public void setPool(GenericKeyedObjectPool<InetSocketAddress, Socket> pool) {
		this.pool = pool;
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

		public OpenTSDBWriter build() throws LifecycleException, UnknownHostException {
			return new OpenTSDBWriter(
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
