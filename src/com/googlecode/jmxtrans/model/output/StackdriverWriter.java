package com.googlecode.jmxtrans.model.output;

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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * <a href="https://www.stackdriver.com//">Stackdriver</a> implementation of the
 * {@linkplain com.googlecode.jmxtrans.OutputWriter}.
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
 * <li>"{@code stackdriverApiTimeoutInMillis}": read timeout of the calls to Stackdriver HTTP API. Optional, default
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
	private String instanceId;
	
	/**
	 *  Prefix sent in the settings of this one writer.  Will be prepended before the metric names that are sent 
	 *  to Stackdriver with a period in between.  Should be alphanumeric [A-Za-z0-9] with no punctuation or spaces.
	 */
	private String prefix;
	
	/**
	 * The gateway URL to post metrics to, this can be overridden for testing locally but should generally be
	 * left at the default.
	 * 
	 * @see #DEFAULT_STACKDRIVER_API_URL
	 */
	private URL gatewayUrl;

	/**
	 * A Proxy object that can be set using the proxyHost and proxyPort settings if the server can't post directly 
	 * to the gateway
	 */
	private Proxy proxy;
	
	/**
	 * Stackdriver API key generated in the account settings section on Stackdriver.  Mandatory for data to be
	 * recognized in the Stackdriver gateway.
	 */
	private String apiKey;
	
	private int stackdriverApiTimeoutInMillis = DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS;

	private JsonFactory jsonFactory = new JsonFactory();
	
	/**
	 * Sets up the object and makes sure all the required parameters are available<br/>
	 * Minimally a Stackdriver API key must be provided using the token setting
	 */
	@Override
	public void validateSetup(Query query) throws ValidationException {
		try {
			gatewayUrl = new URL(getStringSetting(SETTING_STACKDRIVER_API_URL, DEFAULT_STACKDRIVER_API_URL));
		} catch (MalformedURLException e) {
			throw new ValidationException("Invalid gateway URL passed " + gatewayUrl, query);
		}

		apiKey = getStringSetting(SETTING_STACKDRIVER_API_KEY, null);

		if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST, null).isEmpty()) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST, null), getIntSetting(SETTING_PROXY_PORT, 0)));
		}
		
		if (getStringSetting(SETTING_PREFIX, null) != null && !getStringSetting(SETTING_PREFIX, null).isEmpty()) {
			if (!StringUtils.isAlphanumeric(getStringSetting(SETTING_PREFIX, null))) {
				throw new ValidationException("Prefix setting must be alphanumeric only [A-Za-z0-9]", query);
			}
			prefix = getStringSetting(SETTING_PREFIX, null);
			logger.info("Setting prefix to " + prefix);
		}
		
		logger.info("Starting Stackdriver writer connected to '{}', proxy {} ...", gatewayUrl, proxy);
		
		stackdriverApiTimeoutInMillis = getIntSetting(SETTING_STACKDRIVER_API_TIMEOUT_IN_MILLIS, DEFAULT_STACKDRIVER_API_TIMEOUT_IN_MILLIS);

		// try to get and instance ID
		if (getStringSetting(SETTING_SOURCE_INSTANCE, null) != null && !getStringSetting(SETTING_SOURCE_INSTANCE, null).isEmpty()) {
			// if one is set directly use that
			instanceId = getStringSetting(SETTING_SOURCE_INSTANCE, null);
			logger.info("Using instance ID {} from setting {}", instanceId, SETTING_SOURCE_INSTANCE);
		} else if (getStringSetting(SETTING_DETECT_INSTANCE, null) != null && "AWS".equalsIgnoreCase(getStringSetting(SETTING_DETECT_INSTANCE, null))) {
			// if setting is to detect, look on the local machine URL
			logger.info("Detect instance set to AWS, trying to determine AWS instance ID");
			instanceId = getLocalAwsInstanceId();
			if (instanceId != null) {
				logger.info("Detected instance ID as {}", instanceId);
			} else {
				logger.info("Unable to detect AWS instance ID for this machine, sending metrics without an instance ID");
			}
		} else {
			// no instance ID, the metrics will be sent as "bare" custom metrics and not associated with an instance
			instanceId = null;
			logger.info("No source instance ID passed, and not set to detect, sending metrics without and instance ID");
		}
	}
	
	/**
	 * Implementation of the base writing method.  Operates in two stages:
	 * <br/>
	 * First turns the query result into a JSON message in Stackdriver format
	 * <br/>
	 * Second posts the message to the Stackdriver gateway via HTTP
	 */
	@Override
	public void doWrite(Query query) throws Exception {
		String gatewayMessage = getGatewayMessage(query.getResults());
		
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
		JsonGenerator g = jsonFactory.createJsonGenerator(writer);
		g.writeStartObject();
		g.writeNumberField("timestamp", System.currentTimeMillis() / 1000);
		g.writeNumberField("proto_version", STACKDRIVER_PROTOCOL_VERSION);
		g.writeArrayFieldStart("data");

		for (Result metric : results) {
			Map<String, Object> values = metric.getValues();
			if (values != null) {
				for (Entry<String, Object> entry : values.entrySet()) {
					if (JmxUtils.isNumeric(entry.getValue())) {
						// we have a numeric value, write a value into the message
						valueCount++;
						g.writeStartObject();
						if (this.prefix != null) {
							g.writeStringField("name", prefix + "." + metric.getClassName() + "." + metric.getAttributeName() + "." + entry.getKey());
						} else {
							g.writeStringField("name", metric.getClassName() + "." + metric.getAttributeName() + "." + entry.getKey());
						}
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
			urlConnection.setReadTimeout(stackdriverApiTimeoutInMillis);
			urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
			urlConnection.setRequestProperty("x-stackdriver-apikey", apiKey);

			urlConnection.getOutputStream().write(gatewayMessage.getBytes());
			
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
	 * Use the EC2 Metadata URL to determine the instance ID that this code is running on. Useful if you don't want to
	 * configure the instance ID manually. Pass detectInstance = "AWS" to have this run in your configuration.
	 * 
	 * @return String containing an AWS instance id, or null if none is found
	 */
	private String getLocalAwsInstanceId() {
		String detectedInstanceId = null;
		try {
			String inputLine = null;
			final URL metadataUrl = new URL("http://169.254.169.254/latest/meta-data/instance-id");
			URLConnection metadataConnection = metadataUrl.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(metadataConnection.getInputStream(), "UTF-8"));
			while ((inputLine = in.readLine()) != null) {
				detectedInstanceId = inputLine;
			}
			in.close();
		} catch (Exception e) {
			logger.warn("unable to determine AWS instance ID", e);
		}
		return detectedInstanceId;
	}
}
