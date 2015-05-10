package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Charsets.ISO_8859_1;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang.StringUtils.isAlphanumeric;

/**
 * <a href="https://www.stackdriver.com//">Stackdriver</a> implementation of the
 * {@linkplain com.googlecode.jmxtrans.model.OutputWriter}.
 * <p/>
 * This implementation uses <a href="https://custom-gateway.stackdriver.com/v1/custom"> POST {@code /v1/metrics}</a>
 * HTTP API.
 * <p/>
 * Settings:
 * <ul>
 * <li>"{@code url}": Stackdriver server URL. Optional, default value: {@value #DEFAULT_STACKDRIVER_API_URL}.</li>
 * <li>"{@code token}": Stackdriver API token. Mandatory</li>
 * <li>"{@code prefix}": Prefix for the metric names.  If present will be prepended to the metric name.  Should be alphanumeric.  
 * Optional, shouldn't be used at the same time as source or detectInstance.  Different way of namespacing.</li>
 * <li>"{@code source}": Instance of the machine ID that the JMX data is being collected from. Optional.
 * <li>"{@code detectInstance}": Set to "AWS" if you want to detect the local AWS instance ID on startup.  Optional. 
 * <li>"{@code timeoutInMillis}": read timeout of the calls to Stackdriver HTTP API. Optional, default
 * value: {@value #DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS}.</li>
 * <li>"{@code enabled}": flag to enable/disable the writer. Optional, default value: <code>true</code>.</li>
 * </ul>
 * 
 * @author <a href="mailto:eric@stackdriver.com">Eric Kilby</a>
 */
public class StackdriverWriter extends BaseOutputWriter {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	// constant protocol version, this can be updated in future versions for protocol changes
	public static final int STACKDRIVER_PROTOCOL_VERSION = 1;
	
	// defaults for values that can be overridden in settings
	public static final int DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS = 1000;
	
	public static final String DEFAULT_STACKDRIVER_API_URL = "https://custom-gateway.stackdriver.com/v1/custom";
	
	// names of settings
	public static final String SETTING_STACKDRIVER_API_URL = "url";
	
	public final static String SETTING_PROXY_PORT = "proxyPort";
	
	public final static String SETTING_PROXY_HOST = "proxyHost";
	
	public static final String SETTING_STACKDRIVER_API_KEY = "token";
	
	public static final String SETTING_SOURCE_INSTANCE = "source";
	
	public static final String SETTING_DETECT_INSTANCE = "detectInstance";
	
	public static final String SETTING_STACKDRIVER_API_TIMEOUT_IN_MILLIS = "stackdriverApiTimeoutInMillis";
	
	public static final String SETTING_PREFIX = "prefix";
	
	/**
	 * The instance ID that metrics from this writer should be associated with in Stackdriver, an example of this
	 * would be an EC2 instance ID in the form i-00000000 that is present in your environment.
	 */
	private final String instanceId;
	private final String source;
	private final String detectInstance;
	
	/**
	 *  Prefix sent in the settings of this one writer.  Will be prepended before the metric names that are sent 
	 *  to Stackdriver with a period in between.  Should be alphanumeric [A-Za-z0-9] with no punctuation or spaces.
	 */
	private final String prefix;
	
	/**
	 * The gateway URL to post metrics to, this can be overridden for testing locally but should generally be
	 * left at the default.
	 * 
	 * @see #DEFAULT_STACKDRIVER_API_URL
	 */
	private final URL gatewayUrl;

	/**
	 * A Proxy object that can be set using the proxyHost and proxyPort settings if the server can't post directly 
	 * to the gateway
	 */
	private final Proxy proxy;
	private final String proxyHost;
	private final Integer proxyPort;
	
	/**
	 * Stackdriver API key generated in the account settings section on Stackdriver.  Mandatory for data to be
	 * recognized in the Stackdriver gateway.
	 */
	private final String apiKey;
	
	private int timeoutInMillis = DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS;

	private JsonFactory jsonFactory = new JsonFactory();

	@JsonCreator
	public StackdriverWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("gatewayUrl") String gatewayUrl,
			@JsonProperty("apiKey") String apiKey,
			@JsonProperty("proxyHost") String proxyHost,
			@JsonProperty("proxyPort") Integer proxyPort,
			@JsonProperty("prefix") String prefix,
			@JsonProperty("timeoutInMillis") Integer timeoutInMillis,
			@JsonProperty("source") String source,
			@JsonProperty("detectInstance") String detectInstance,
			@JsonProperty("settings") Map<String, Object> settings) throws MalformedURLException {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.gatewayUrl = new URL(firstNonNull(
				gatewayUrl,
				(String) getSettings().get(SETTING_STACKDRIVER_API_URL),
				DEFAULT_STACKDRIVER_API_URL));
		this.apiKey = MoreObjects.firstNonNull(apiKey, (String) getSettings().get(SETTING_STACKDRIVER_API_KEY));

		// Proxy configuration
		if (proxyHost == null) {
			proxyHost = (String) getSettings().get(SETTING_PROXY_HOST);
		}
		if (proxyPort == null) {
			proxyPort = (Integer) getSettings().get(SETTING_PROXY_PORT);
		}

		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;

		if (!isNullOrEmpty(this.proxyHost)) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, this.proxyPort));
		} else {
			proxy = null;
		}

		// Prefix
		this.prefix = firstNonNull(prefix, (String) getSettings().get(SETTING_PREFIX), "");
		if (!isNullOrEmpty(this.prefix)) {
			if (!isAlphanumeric(this.prefix)) {
				throw new IllegalArgumentException("Prefix setting must be alphanumeric only [A-Za-z0-9]");
			}
		}
		logger.info("Setting prefix to " + this.prefix);

		this.timeoutInMillis = firstNonNull(
				timeoutInMillis,
				Settings.getIntegerSetting(getSettings(), SETTING_STACKDRIVER_API_TIMEOUT_IN_MILLIS, null),
				DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS);

		// try to get and instance ID
		if (source == null) {
			source = (String) getSettings().get(SETTING_SOURCE_INSTANCE);
		}
		this.source = source;
		if (detectInstance == null) {
			detectInstance = (String) getSettings().get(SETTING_DETECT_INSTANCE);
		}
		this.detectInstance = detectInstance;
		this.instanceId = computeInstanceId(this.source, this.detectInstance);
	}

	/**
	 * Sets up the object and makes sure all the required parameters are available<br/>
	 * Minimally a Stackdriver API key must be provided using the token setting
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		logger.info("Starting Stackdriver writer connected to '{}', proxy {} ...", gatewayUrl, proxy);
	}

	private String computeInstanceId(String source, String detectInstance) {
		String result;
		if (!isNullOrEmpty(source)) {
			// if one is set directly use that
			result = source;
			logger.info("Using instance ID {} from setting {}", result, SETTING_SOURCE_INSTANCE);
		} else {
			if ("AWS".equalsIgnoreCase(detectInstance)) {
				// if setting is to detect, look on the local machine URL
				logger.info("Detect instance set to AWS, trying to determine AWS instance ID");
				result = getLocalInstanceId("AWS", "http://169.254.169.254/latest/meta-data/instance-id", null);
				if (result != null) {
				} else {
					logger.info("Unable to detect AWS instance ID for this machine, sending metrics without an instance ID");
				}
			} else if ("GCE".equalsIgnoreCase(detectInstance)) {
				// if setting is to detect, look on the local machine URL
				logger.info("Detect instance set to GCE, trying to determine GCE instance ID");
				result = getLocalInstanceId("GCE", "http://metadata/computeMetadata/v1/instance/id", ImmutableMap.of("X-Google-Metadata-Request", "True"));
				if (result == null) {
					logger.info("Unable to detect GCE instance ID for this machine, sending metrics without an instance ID");
				}
			} else {
				// no instance ID, the metrics will be sent as "bare" custom metrics and not associated with an instance
				result = null;
				logger.info("No source instance ID passed, and not set to detect, sending metrics without an instance ID");
			}
		}
		logger.info("Detected instance ID as {}", result);
		return result;
	}

	/**
	 * Implementation of the base writing method.  Operates in two stages:
	 * <br/>
	 * First turns the query result into a JSON message in Stackdriver format
	 * <br/>
	 * Second posts the message to the Stackdriver gateway via HTTP
	 */
	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		String gatewayMessage = getGatewayMessage(results);
		
		// message won't be returned if there are no numeric values in the query results
		if (gatewayMessage != null) {
			logger.info(gatewayMessage);
			doSend(gatewayMessage);
		}
	}

	/**
	 * Take query results, make a JSON String
	 * 
	 * @param results List of Result objects
	 * @return a String containing a JSON message, or null if there are no values to report
	 * 
	 * @throws IOException if there is some problem generating the JSON, should be uncommon
	 */
	private String getGatewayMessage(final List<Result> results) throws IOException {
		int valueCount = 0;
		Writer writer = new StringWriter();
		JsonGenerator g = jsonFactory.createGenerator(writer);
		g.writeStartObject();
		g.writeNumberField("timestamp", System.currentTimeMillis() / 1000);
		g.writeNumberField("proto_version", STACKDRIVER_PROTOCOL_VERSION);
		g.writeArrayFieldStart("data");

		List<String> typeNames = this.getTypeNames();
		
		for (Result metric : results) {
			Map<String, Object> values = metric.getValues();
			if (values != null) {
				for (Entry<String, Object> entry : values.entrySet()) {
					if (NumberUtils.isNumeric(entry.getValue())) {
						// we have a numeric value, write a value into the message
						
						StringBuilder nameBuilder = new StringBuilder();
						
						// put the prefix if set
						if (this.prefix != null) {
							nameBuilder.append(prefix);
							nameBuilder.append(".");
						}
						
						// put the class name or its alias if available
						if (!metric.getKeyAlias().isEmpty()) {
							nameBuilder.append(metric.getKeyAlias());
							
						} else {
							nameBuilder.append(metric.getClassName());	
						}
						
						// Wildcard "typeNames" substitution
						String typeName = com.googlecode.jmxtrans.model.naming.StringUtils.cleanupStr(KeyUtils.getConcatedTypeNameValues(typeNames, metric.getTypeName()));
						if (typeName != null && typeName.length() > 0) {
							nameBuilder.append(".");
							nameBuilder.append(typeName);
						}
						
						// add the attribute name
						nameBuilder.append(".");
						nameBuilder.append(metric.getAttributeName());
						
						// put the value name if it differs from the attribute name
						if (!entry.getKey().equals(metric.getAttributeName())) {
							nameBuilder.append(".");
							nameBuilder.append(entry.getKey());
						}
						
						// check for Float/Double NaN since these will cause the message validation to fail 
						if (entry.getValue() instanceof Float && ((Float) entry.getValue()).isNaN()) {
							logger.info("Metric value for " + nameBuilder.toString() + " is NaN, skipping");
							continue;
						}
						
						if (entry.getValue() instanceof Double && ((Double) entry.getValue()).isNaN()) {
							logger.info("Metric value for " + nameBuilder.toString() + " is NaN, skipping");
							continue;
						}
						
						valueCount++;
						g.writeStartObject();
						
						g.writeStringField("name", nameBuilder.toString());
						
						g.writeNumberField("value", Double.valueOf(entry.getValue().toString()));
						
						// if the metric is attached to an instance, include that in the message
						if (instanceId != null && !instanceId.isEmpty()) {
							g.writeStringField("instance", instanceId);
						}
						g.writeNumberField("collected_at", metric.getEpoch() / 1000);
						g.writeEndObject();
					}
				}
			}
		}
		
		g.writeEndArray();
		g.writeEndObject();
		g.flush();
		g.close();
		
		// return the message if there are any values to report
		if (valueCount > 0) {
			return writer.toString();
		} else {
			return null;
		}
	}
	
	/**
	 * Post the formatted results to the gateway URL over HTTP 
	 * 
	 * @param gatewayMessage String in the Stackdriver custom metrics JSON format containing the data points
	 */
	private void doSend(final String gatewayMessage) {
		HttpURLConnection urlConnection = null;

		try {
			if (proxy == null) {
				urlConnection = (HttpURLConnection) gatewayUrl.openConnection();
			} else {
				urlConnection = (HttpURLConnection) gatewayUrl.openConnection(proxy);
			}
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			urlConnection.setReadTimeout(timeoutInMillis);
			urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
			urlConnection.setRequestProperty("x-stackdriver-apikey", apiKey);

			// Stackdriver's own implementation does not specify char encoding
			// to use. Let's take the simplest approach and at lest ensure that
			// if we have problems they can be reproduced in consistant ways.
			// See https://github.com/Stackdriver/stackdriver-custommetrics-java/blob/master/src/main/java/com/stackdriver/api/custommetrics/CustomMetricsPoster.java#L262
			// for details.
			urlConnection.getOutputStream().write(gatewayMessage.getBytes(ISO_8859_1));
			
			int responseCode = urlConnection.getResponseCode();
			if (responseCode != 200 && responseCode != 201) {
				logger.warn("Failed to send results to Stackdriver server: responseCode=" + responseCode + " message=" + urlConnection.getResponseMessage());
			}
		} catch (Exception e) {
			logger.warn("Failure to send result to Stackdriver server", e);
		} finally {
			if (urlConnection != null) {
				try {
					InputStream in = urlConnection.getInputStream();
					in.close();
					InputStream err = urlConnection.getErrorStream();
					if (err != null) {
						err.close();
					}
					urlConnection.disconnect();
				} catch (IOException e) {
					logger.warn("Error flushing http connection for one result, continuing");
					logger.debug("Stack trace for the http connection, usually a network timeout", e);
				}
			}

		}
	}
	
	/**
	 * Use a Cloud provider local metadata endpoint to determine the instance ID that this code is running on. 
	 * Useful if you don't want to configure the instance ID manually. 
	 * Pass detectInstance param with a cloud provider ID (AWS|GCE) to have this run in your configuration.
	 * 
	 * @return String containing an instance id, or null if none is found
	 */
	private String getLocalInstanceId(final String cloudProvider, final String metadataEndpoint, final Map<String,String> headers) {
		String detectedInstanceId = null;
		try {
			final URL metadataUrl = new URL(metadataEndpoint);
			URLConnection metadataConnection = metadataUrl.openConnection();
			// add any additional headers passed in
			if (headers != null) {
				for (Map.Entry<String, String> header : headers.entrySet()) {
					metadataConnection.setRequestProperty(header.getKey(), header.getValue());
				}
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(metadataConnection.getInputStream(), "UTF-8"));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				detectedInstanceId = inputLine;
			}
			in.close();
		} catch (Exception e) {
			logger.warn("unable to determine " + cloudProvider + " instance ID", e);
		}
		return detectedInstanceId;
	}

	public String getGatewayUrl() {
		return gatewayUrl.toString();
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getApiKey() {
		return apiKey;
	}

	public int getTimeoutInMillis() {
		return timeoutInMillis;
	}

	public String getSource() {
		return source;
	}

	public String getDetectInstance() {
		return detectInstance;
	}
}
