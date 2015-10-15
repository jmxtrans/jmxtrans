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
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.naming.StringUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This writer is a port of the LibratoWriter from the embedded-jmxtrans
 * project.
 * <p/>
 * <a href="https://metrics.librato.com//">Librato Metrics</a>
 * <p/>
 * This implementation uses <a href="http://dev.librato.com/v1/post/metrics">
 * POST {@code /v1/metrics}</a> HTTP API.
 * <p/>
 * Settings:
 * <ul>
 * <li>"{@code url}": Librato server URL. Optional, default value:
 * {@value #DEFAULT_LIBRATO_API_URL}.</li>
 * <li>"{@code username}": Librato username. Mandatory</li>
 * <li>"{@code token}": Librato token. Mandatory</li>
 * <li>"{@code libratoApiTimeoutInMillis}": read timeout of the calls to Librato
 * HTTP API. Optional, default value: 1000.</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class LibratoWriter extends BaseOutputWriter {

	public final static String SETTING_URL = "url";
	public final static String SETTING_USERNAME = "username";
	public final static String SETTING_TOKEN = "token";
	public final static String SETTING_PROXY_HOST = "proxyHost";
	public final static String SETTING_PROXY_PORT = "proxyPort";
	public static final String DEFAULT_LIBRATO_API_URL = "https://metrics-api.librato.com/v1/metrics";
	public static final String SETTING_LIBRATO_API_TIMEOUT_IN_MILLIS = "libratoApiTimeoutInMillis";

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	private final JsonFactory jsonFactory = new JsonFactory();
	/**
	 * Librato HTTP API URL
	 */
	private final URL url;
	private final int libratoApiTimeoutInMillis;
	/**
	 * Librato HTTP API authentication username
	 */
	private final String username;
	private final String token;
	private final String basicAuthentication;
	/**
	 * Optional proxy for the http API calls
	 */
	private final String proxyHost;
	private final Integer proxyPort;
	private Proxy proxy;
	
	@VisibleForTesting
	final String httpUserAgent;

	@JsonCreator
	public LibratoWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("url") URL url,
			@JsonProperty("libratoApiTimeoutInMillis") Integer libratoApiTimeoutInMillis,
			@JsonProperty("username") String username,
			@JsonProperty("token") String token,
			@JsonProperty("proxyHost") String proxyHost,
			@JsonProperty("proxyPort") Integer proxyPort,
			@JsonProperty("settings") Map<String, Object> settings) throws MalformedURLException {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.url = MoreObjects.firstNonNull(
				url,
				new URL(MoreObjects.firstNonNull(
						(String) this.getSettings().get(SETTING_URL),
						DEFAULT_LIBRATO_API_URL)));
		this.libratoApiTimeoutInMillis = MoreObjects.firstNonNull(
				libratoApiTimeoutInMillis,
				Settings.getIntSetting(getSettings(), SETTING_LIBRATO_API_TIMEOUT_IN_MILLIS, 1000));
		this.username = MoreObjects.firstNonNull(
				username,
				(String) getSettings().get(SETTING_USERNAME));
		this.token = MoreObjects.firstNonNull(
				token,
				(String) getSettings().get(SETTING_TOKEN));
		this.basicAuthentication = Base64Variants
				.getDefaultVariant()
				.encode((this.username + ":" + this.token).getBytes(Charsets.US_ASCII));
		this.proxyHost = proxyHost != null ? proxyHost : (String) getSettings().get(SETTING_PROXY_HOST);
		this.proxyPort = proxyPort != null ? proxyPort : Settings.getIntegerSetting(getSettings(), SETTING_PROXY_PORT, null);
		if (this.proxyHost != null && this.proxyPort != null) {
			this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, this.proxyPort));
		} else {
			this.proxy = null;
		}
		this.httpUserAgent =
				"jmxtrans-standalone/1 " + "(" +
						System.getProperty("java.vm.name") + "/" + System.getProperty("java.version") + "; " +
						System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "/" + System.getProperty("os.version")
						+ ")";
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
		logger.info("Start Librato writer connected to '{}', proxy {} with username '{}' ...", url, proxy, username);
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		logger.debug("Export to '{}', proxy {} metrics {}", url, proxy, query);
		writeToLibrato(server, query, results);
	}

	private void serialize(Server server, Query query, List<Result> results, OutputStream outputStream) throws IOException {
		JsonGenerator g = jsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);
		g.writeStartObject();
		g.writeArrayFieldStart("counters");
		g.writeEndArray();

		String source = getSource(server);

		g.writeArrayFieldStart("gauges");
		List<String> typeNames = getTypeNames();
		for (Result result : results) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Map.Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {
						g.writeStartObject();
						g.writeStringField("name", KeyUtils.getKeyString(query, result, values, typeNames));
						if (source != null && !source.isEmpty()) {
							g.writeStringField("source", source);
						}
						g.writeNumberField("measure_time", TimeUnit.SECONDS.convert(result.getEpoch(), TimeUnit.MILLISECONDS));
						Object value = values.getValue();
						if (value instanceof Integer) {
							g.writeNumberField("value", (Integer) value);
						} else if (value instanceof Long) {
							g.writeNumberField("value", (Long) value);
						} else if (value instanceof Float) {
							g.writeNumberField("value", (Float) value);
						} else if (value instanceof Double) {
							g.writeNumberField("value", (Double) value);
						}
						g.writeEndObject();
					}
				}
			}
		}
		g.writeEndArray();
		g.writeEndObject();
		g.flush();
		g.close();

	}

	private void writeToLibrato(Server server, Query query, List<Result> results) {
		HttpURLConnection urlConnection = null;
		try {
			if (proxy == null) {
				urlConnection = (HttpURLConnection) url.openConnection();
			} else {
				urlConnection = (HttpURLConnection) url.openConnection(proxy);
			}
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			urlConnection.setReadTimeout(libratoApiTimeoutInMillis);
			urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
			urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);
			urlConnection.setRequestProperty("User-Agent", httpUserAgent);

			serialize(server, query, results, urlConnection.getOutputStream());
			int responseCode = urlConnection.getResponseCode();
			if (responseCode != 200) {
				logger.warn("Failure {}:'{}' to send result to Librato server '{}' with proxy {}, username {}", responseCode, urlConnection.getResponseMessage(), url, proxy, username);
			}
			if (logger.isTraceEnabled()) {
				IOUtils.copy(urlConnection.getInputStream(), System.out);
			}
		} catch (Exception e) {
			logger.warn("Failure to send result to Librato server '{}' with proxy {}, username {}", url, proxy, username, e);
		} finally {
			if (urlConnection != null) {
				try {
					InputStream in = urlConnection.getInputStream();
					IOUtils.copy(in, NullOutputStream.NULL_OUTPUT_STREAM);
					IOUtils.closeQuietly(in);
					InputStream err = urlConnection.getErrorStream();
					if (err != null) {
						IOUtils.copy(err, NullOutputStream.NULL_OUTPUT_STREAM);
						IOUtils.closeQuietly(err);
					}
				} catch (IOException e) {
					logger.warn("Exception flushing http connection", e);
				}
			}

		}
	}

	private String getSource(Server server) {
		if (server.getAlias() != null) {
			return server.getAlias();
		} else {
			return StringUtils.cleanupStr(server.getHost());
		}
	}

	public URL getUrl() {
		return url;
	}

	public int getLibratoApiTimeoutInMillis() {
		return libratoApiTimeoutInMillis;
	}

	public String getUsername() {
		return username;
	}

	public String getToken() {
		return token;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}
}
