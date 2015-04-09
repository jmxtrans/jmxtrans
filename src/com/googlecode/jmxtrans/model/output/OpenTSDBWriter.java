package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

/**
 * OpenTSDBWriter which directly sends
 * <p/>
 * Created from sources originally written by Balazs Kossovics <bko@witbe.net>
 */

public class OpenTSDBWriter extends OpenTSDBGenericWriter {
	private static final Logger log = LoggerFactory.getLogger(OpenTSDBWriter.class);

	protected Socket socket;
	protected DataOutputStream out;

	@JsonCreator
	public OpenTSDBWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("useObjDomain") Boolean useObjDomain,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("tags") Map<String, String> tags,
			@JsonProperty("tagName") String tagName,
			@JsonProperty("mergeTypeNamesTags") Boolean mergeTypeNamesTags,
			@JsonProperty("metricNamingExpression") String metricNamingExpression,
			@JsonProperty("addHostnameTag") Boolean addHostnameTag,
			@JsonProperty("settings") Map<String, Object> settings) throws LifecycleException, UnknownHostException {
		super(typeNames, booleanAsNumber, debugEnabled, useObjDomain, host, port, tags, tagName, mergeTypeNamesTags, metricNamingExpression,
				addHostnameTag, settings);
	}

	/**
	 * Add the hostname tag "host" with the name of the host by default since OpenTSDB otherwise does not have this
	 * information.
	 */
	@Override
	protected boolean getAddHostnameTagDefault() {
		return true;
	}

	/**
	 * Prepare for sending metrics.
	 */
	@Override
	protected void prepareSender() throws LifecycleException {

		if (host == null || port == null) {
			throw new LifecycleException("Host and port for " + this.getClass().getSimpleName() + " output can't be null");
		}

		try {
			socket = new Socket(host, port);
		} catch (UnknownHostException e) {
			log.error("error opening socket to OpenTSDB", e);
			throw new LifecycleException(e);
		} catch (IOException e) {
			log.error("error opening socket to OpenTSDB", e);
			throw new LifecycleException(e);
		}
	}

	/**
	 * Shutdown the sender as it will no longer be used to send metrics.
	 */
	@Override
	protected void shutdownSender() throws LifecycleException {
		try {
			socket.close();
		} catch (IOException e) {
			log.error("error closing socket to OpenTSDB", e);
			throw new LifecycleException(e);
		}
	}

	/**
	 * Start the output for the results of a Query to OpenTSDB.
	 */
	@Override
	protected void startOutput() throws IOException {
		try {
			this.out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			log.error("error getting the output stream", e);
			throw e;
		}
	}

	/**
	 * Send a single metric to the server.
	 *
	 * @param metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
	 *                   "put" keyword expected by OpenTSDB and the trailing newline character.
	 */
	@Override
	protected void sendOutput(String metricLine) throws IOException {
		try {
			this.out.writeBytes("put " + metricLine + "\n");
		} catch (IOException e) {
			log.error("error writing result to the output stream", e);
			throw e;
		}
	}

	/**
	 * Finish the output for a single Query, flushing all data to the server and logging the server's response.
	 */
	@Override
	protected void finishOutput() throws IOException {
		try {
			this.out.flush();
		} catch (IOException e) {
			log.error("flush failed", e);
			throw e;
		}

		// Read and log the response from the server for diagnostic purposes.

		InputStreamReader socketInputStream = new InputStreamReader(socket.getInputStream(), UTF_8);
		BufferedReader bufferedSocketInputStream = new BufferedReader(socketInputStream);
		String line;
		while (socketInputStream.ready() && (line = bufferedSocketInputStream.readLine()) != null) {
			log.warn("OpenTSDB says: " + line);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final ImmutableList.Builder<String> typeNames = ImmutableList.builder();
		private boolean booleanAsNumber;
		private Boolean debugEnabled;
		private Boolean useObjDomain;
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
		
		public Builder setUseObjDomain(boolean useObjDomain) {
			this.useObjDomain = useObjDomain;
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
					useObjDomain,
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
