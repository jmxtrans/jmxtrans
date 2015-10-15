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
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;

/**
 * This low latency and thread safe output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">Getting your data into Graphite</a>
 *
 * @author jon
 */
@NotThreadSafe
public class GraphiteWriter extends BaseOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);

	private static final String DEFAULT_ROOT_PREFIX = "servers";

	private GenericKeyedObjectPool<InetSocketAddress, Socket> pool;

	private final String rootPrefix;
	private final InetSocketAddress address;

	@JsonCreator
	public GraphiteWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.rootPrefix = resolveProps(
				firstNonNull(
						rootPrefix,
						(String) getSettings().get("rootPrefix"),
						DEFAULT_ROOT_PREFIX));
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

	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		Socket socket = null;
		PrintWriter writer = null;

		try {
			socket = pool.borrowObject(address);
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

			List<String> typeNames = this.getTypeNames();

			for (Result result : results) {
				log.debug("Query result: {}", result);
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						Object value = values.getValue();
						if (NumberUtils.isNumeric(value)) {

							String line = KeyUtils.getKeyString(server, query, result, values, typeNames, rootPrefix)
									.replaceAll("[()]", "_") + " " + value.toString() + " "
									+ result.getEpoch() / 1000 + "\n";
							log.debug("Graphite Message: {}", line);
							writer.write(line);
						} else {
							log.warn("Unable to submit non-numeric value to Graphite: [{}] from result [{}]", value, result);
						}
					}
				}
			}
		} finally {
			if (writer != null && writer.checkError()) {
				log.error("Error writing to Graphite, clearing Graphite socket pool");
				pool.invalidateObject(address, socket);
			} else {
				pool.returnObject(address, socket);
			}
		}
	}

	public String getHost() {
		return address.getHostName();
	}

	public int getPort() {
		return address.getPort();
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
		private String rootPrefix;
		private String host;
		private Integer port;

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

		public Builder setRootPrefix(String rootPrefix) {
			this.rootPrefix = rootPrefix;
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

		public GraphiteWriter build() {
			return new GraphiteWriter(
					typeNames.build(),
					booleanAsNumber,
					debugEnabled,
					rootPrefix,
					host,
					port,
					null
			);
		}
	}
}
