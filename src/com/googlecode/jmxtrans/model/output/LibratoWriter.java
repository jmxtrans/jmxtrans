package com.googlecode.jmxtrans.model.output;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.codehaus.jackson.Base64Variants;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import com.googlecode.jmxtrans.util.ValidationException;

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
 * <li>"{@code user}": Librato user. Mandatory</li>
 * <li>"{@code token}": Librato token. Mandatory</li>
 * <li>"{@code libratoApiTimeoutInMillis}": read timeout of the calls to Librato
 * HTTP API. Optional, default value:
 * {@value #DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS}.</li>
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
	public static final int DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS = 1000;

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	private final JsonFactory jsonFactory = new JsonFactory();
	/**
	 * Librato HTTP API URL
	 */
	private URL url;
	private int libratoApiTimeoutInMillis = DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS;
	/**
	 * Librato HTTP API authentication username
	 */
	private String user;
	/**
	 * Librato HTTP API authentication token
	 */
	private String token;
	private String basicAuthentication;
	/**
	 * Optional proxy for the http API calls
	 */
	private Proxy proxy;

	@Override
	public void start() {

	}

	public void validateSetup(Query query) throws ValidationException {
		try {
			url = new URL(getStringSetting(SETTING_URL, DEFAULT_LIBRATO_API_URL));

			user = getStringSetting(SETTING_USERNAME);
			token = getStringSetting(SETTING_TOKEN);
			basicAuthentication = Base64Variants.getDefaultVariant().encode((user + ":" + token).getBytes(Charset.forName("US-ASCII")));

			if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST, "").isEmpty()) {
				proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST, null), getIntegerSetting(SETTING_PROXY_PORT, null)));
			}

			libratoApiTimeoutInMillis = getIntSetting(SETTING_LIBRATO_API_TIMEOUT_IN_MILLIS, DEFAULT_LIBRATO_API_TIMEOUT_IN_MILLIS);

			logger.info("Start Librato writer connected to '{}', proxy {} with user '{}' ...", url, proxy, user);
		} catch (MalformedURLException ex) {
			logger.error("Error :", ex);
		}
	}

	public void doWrite(Query query, List<Result> results) throws Exception {
		logger.debug("Export to '{}', proxy {} metrics {}", url, proxy, query);
		writeToLibrato(query, results);
	}

	private void serialize(Query query, List<Result> results, OutputStream outputStream) throws IOException {
		JsonGenerator g = jsonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
		g.writeStartObject();
		g.writeArrayFieldStart("counters");
		g.writeEndArray();

		String source = getSource(query);

		g.writeArrayFieldStart("gauges");
		List<String> typeNames = getTypeNames();
		for (Result result : results) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Map.Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {
						g.writeStartObject();
						g.writeStringField("name", JmxUtils.getKeyStringWithDottedKeys(query, result, values, typeNames));
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

	private void writeToLibrato(Query query, List<Result> results) {
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

			serialize(query, results, urlConnection.getOutputStream());
			int responseCode = urlConnection.getResponseCode();
			if (responseCode != 200) {
				logger.warn("Failure {}:'{}' to send result to Librato server '{}' with proxy {}, user {}", responseCode, urlConnection.getResponseMessage(), url, proxy, user);
			}
			if (logger.isTraceEnabled()) {
				IOUtils.copy(urlConnection.getInputStream(), System.out);
			}
		} catch (Exception e) {
			logger.warn("Failure to send result to Librato server '{}' with proxy {}, user {}", url, proxy, user, e);
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

	private String getSource(Query query) {
		if (query.getServer().getAlias() != null) {
			return query.getServer().getAlias();
		} else {
			return cleanupStr(query.getServer().getHost());
		}
	}

	private String getStringSetting(String setting) throws ValidationException {
		String s = super.getStringSetting(setting, null);
		if (s == null) {
			throw new ValidationException("Setting '" + setting + "' cannot be null", null);
		}
		return s;
	}
}
