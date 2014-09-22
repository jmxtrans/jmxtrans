package com.googlecode.jmxtrans.model.output;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
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

	public final static String SETTING_HOST = "host";
	public final static String SETTING_HANDLER = "handler";
	public final static String DEFAULT_SENSU_HOST = "localhost";
	public final static String DEFAULT_SENSU_HANDLER = "graphite";

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	private final JsonFactory jsonFactory = new JsonFactory();
	/**
	 * Sensu HTTP API URL
	 */
	private String sensuhost;
	private String sensuhandler;

	@Override
	public void start() {

	}

	public void validateSetup(Query query) throws ValidationException {
		sensuhost = new String(getStringSetting(SETTING_HOST, DEFAULT_SENSU_HOST));
		sensuhandler = new String(getStringSetting(SETTING_HANDLER, DEFAULT_SENSU_HANDLER));
		logger.info("Start Sensu writer connected to '{}' with handler {}", sensuhost, sensuhandler);
	}

	public void doWrite(Query query) throws Exception {
		logger.debug("Export to '{}', metrics {}", sensuhost, query);
		writeToSensu(query);
	}

	private void serialize(Query query, OutputStream outputStream) throws IOException {
		JsonGenerator g = jsonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		g.writeStringField("name", "jmxtrans");
		g.writeStringField("type", "metric");
		g.writeStringField("handler", sensuhandler);

		String jsonoutput = "";
		List<String> typeNames = getTypeNames();
		for (Result result : query.getResults()) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Map.Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {
						Object value = values.getValue();
						jsonoutput = jsonoutput + JmxUtils.getKeyString(query, result, values, typeNames, null) + " " + value + " " + TimeUnit.SECONDS.convert(result.getEpoch(), TimeUnit.MILLISECONDS) + System.getProperty("line.separator");
					}
				}
			}
		}
		g.writeStringField("output", jsonoutput);
		g.writeEndObject();
		g.flush();
		g.close();
	}

	private void writeToSensu(Query query) {
		Socket socketConnection = null;
		try {
			socketConnection = new Socket(sensuhost, 3030);
			serialize(query, socketConnection.getOutputStream());
		} catch (Exception e) {
			logger.warn("Failure to send result to Sensu server '{}'", sensuhost, e);
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
}
