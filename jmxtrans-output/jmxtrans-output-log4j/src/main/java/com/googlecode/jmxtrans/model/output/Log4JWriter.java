package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.naming.StringUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * A writer for Log4J. It may be a nice way to send JMX metrics to Logstash for example. <br /> <br />
 * <p/>
 * Here is an example of MDC variables that are set by this class. <br> <br> server: localhost_9003 <br /> metric:
 * sun_management_MemoryImpl.HeapMemoryUsage_committed <br /> value: 1251999744 <br /> resultAlias: myHeapMemoryUsage
 * <br /> attributeName: HeapMemoryUsage <br /> key: committed <br /> epoch: 1388343325728 <br />
 *
 * @author Yannick Robin
 */
public class Log4JWriter extends BaseOutputWriter {
	private final Logger log;
	private final String logger;

	@JsonCreator
	public Log4JWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("logger") String logger,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.logger = firstNonNull(logger, (String) getSettings().get("logger"), "Log4JWriter");
		this.log = Logger.getLogger("Log4JWriter." + this.logger);
	}

	public void validateSetup(Server server, final Query query) throws ValidationException {}

	/**
	 * Set the log context and log
	 */
	public void internalWrite(Server server, final Query query, ImmutableList<Result> results) throws Exception {
		final List<String> typeNames = getTypeNames();

		for (final Result result : results) {
			final Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (final Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {
						String alias;
						if (server.getAlias() != null) {
							alias = server.getAlias();
						} else {
							alias = server.getHost() + "_" + server.getPort();
							alias = StringUtils.cleanupStr(alias);
						}

						MDC.put("server", alias);
						MDC.put("metric", KeyUtils.getKeyString(server, query, result, values, typeNames, null));
						MDC.put("value", values.getValue());
						if (result.getKeyAlias() != null) {
							MDC.put("resultAlias", result.getKeyAlias());
						}
						MDC.put("attributeName", result.getAttributeName());
						MDC.put("key", values.getKey());
						MDC.put("Epoch", String.valueOf(result.getEpoch()));
						log.info("");
					}
				}
			}
		}
	}

	public String getLogger() {
		return logger;
	}
}
