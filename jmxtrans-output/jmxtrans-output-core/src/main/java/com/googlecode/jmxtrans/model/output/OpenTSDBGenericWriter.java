package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.NamingStrategy;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.ClassAttributeNamingStrategy;
import com.googlecode.jmxtrans.model.naming.JexlNamingStrategy;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;

/**
 * Originally written by Balazs Kossovics <bko@witbe.net>.  Common base class for OpenTSDBWriter and TCollectorWriter.
 * Date: 4/4/13
 * Time: 6:00 PM
 * <p/>
 * Updates by Arthur Naseef
 */
public abstract class OpenTSDBGenericWriter extends BaseOutputWriter {
	public static final boolean DEFAULT_MERGE_TYPE_NAMES_TAGS = true;

	private static final Logger log = LoggerFactory.getLogger(OpenTSDBGenericWriter.class);

	protected final String host;
	protected final Integer port;
	protected final ImmutableMap<String, String> tags;
	protected final String tagName;
	protected final NamingStrategy metricNameStrategy;

	protected final boolean mergeTypeNamesTags;
	protected final boolean addHostnameTag;
	protected final String hostnameTag;

	@JsonCreator
	public OpenTSDBGenericWriter(
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
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.host = resolveProps(MoreObjects.firstNonNull(host, (String) getSettings().get(HOST)));
		this.port = MoreObjects.firstNonNull(port, Settings.getIntegerSetting(getSettings(), PORT, null));
		this.tags = ImmutableMap.copyOf(firstNonNull(
				tags,
				(Map<String, String>) getSettings().get("tags"),
				ImmutableMap.<String, String>of()));
		this.tagName = firstNonNull(tagName, (String) getSettings().get("tagName"), "type");
		this.mergeTypeNamesTags = MoreObjects.firstNonNull(
				mergeTypeNamesTags,
				Settings.getBooleanSetting(this.getSettings(), "mergeTypeNamesTags", DEFAULT_MERGE_TYPE_NAMES_TAGS));

		String jexlExpr = metricNamingExpression;
		if (jexlExpr == null) {
			jexlExpr = Settings.getStringSetting(this.getSettings(), "metricNamingExpression", null);
		}
		if (jexlExpr != null) {
			try {
				this.metricNameStrategy = new JexlNamingStrategy(jexlExpr);
			} catch (JexlException jexlExc) {
				throw new LifecycleException("failed to setup naming strategy", jexlExc);
			}
		} else {
			this.metricNameStrategy = new ClassAttributeNamingStrategy();
		}

		this.addHostnameTag = firstNonNull(
				addHostnameTag,
				Settings.getBooleanSetting(this.getSettings(), "addHostnameTag", this.getAddHostnameTagDefault()),
				getAddHostnameTagDefault());

		if (this.addHostnameTag) {
			hostnameTag = InetAddress.getLocalHost().getHostName();
		} else {
			hostnameTag = null;
		}
	}

	/**
	 * Prepare for sending metrics, if needed.  For use by subclasses.
	 */
	protected void prepareSender() throws LifecycleException {
	}

	/**
	 * Shutdown the sender, if needed.  For use by subclasses.
	 */
	protected void shutdownSender() throws LifecycleException {
	}

	/**
	 * Prepare a batch of results output, if needed.  For use by subclasses.
	 */
	protected void startOutput() throws IOException {
	}

	/**
	 * Subclass responsibility: specify the default value for the "addHostnameTag" setting.
	 */
	protected abstract boolean getAddHostnameTagDefault();

	/**
	 * Subcall responsibility: method to perform the actual output for the given metric line.  Every subclass
	 * <b>must</b> implement this method.
	 *
	 * @param metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
	 *                   "put" keyword expected by OpenTSDB and the trailing newline character.
	 */
	protected abstract void sendOutput(String metricLine) throws IOException;

	/**
	 * Complete a batch of results output, if needed.  For use by subclasses.
	 */
	protected void finishOutput() throws IOException {
	}

	/**
	 * Add tags to the given result string, including a "host" tag with the name of the server and all of the tags
	 * defined in the "settings" entry in the configuration file within the "tag" element.
	 *
	 * @param resultString - the string containing the metric name, timestamp, value, and possibly other content.
	 */
	void addTags(StringBuilder resultString) {
		if (addHostnameTag && hostnameTag != null) {
			addTag(resultString, "host", hostnameTag);
		}

		if (tags != null) {
			// Add the constant tag names and values.
			for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
				addTag(resultString, tagEntry.getKey(), tagEntry.getValue());
			}
		}
	}

	/**
	 * Add one tag, with the provided name and value, to the given result string.
	 *
	 * @param resultString - the string containing the metric name, timestamp, value, and possibly other content.
	 * @return String - the new result string with the tag appended.
	 */
	void addTag(StringBuilder resultString, String tagName, String tagValue) {
		resultString.append(" ");
		resultString.append(sanitizeString(tagName));
		resultString.append("=");
		resultString.append(sanitizeString(tagValue));
	}

	/**
	 * Format the result string given the class name and attribute name of the source value, the timestamp, and the
	 * value.
	 *
	 * @param epoch - the timestamp of the metric.
	 * @param value - value of the attribute to use as the metric value.
	 * @return String - the formatted result string.
	 */
	void formatResultString(StringBuilder resultString, String metricName, long epoch, Object value) {
		resultString.append(sanitizeString(metricName));
		resultString.append(" ");
		resultString.append(Long.toString(epoch));
		resultString.append(" ");
		resultString.append(sanitizeString(value.toString()));
	}

	/**
	 * Parse one of the results of a Query and return a list of strings containing metric details ready for sending to
	 * OpenTSDB.
	 *
	 * @param result - one results from the Query.
	 * @return List<String> - the list of strings containing metric details ready for sending to OpenTSDB.
	 */
	List<String> resultParser(Result result) {
		List<String> resultStrings = new LinkedList<String>();
		Map<String, Object> values = result.getValues();

		String attributeName = result.getAttributeName();

		if (values.containsKey(attributeName) && values.size() == 1) {
			processOneMetric(resultStrings, result, values.get(attributeName), null, null);
		} else {
			for (Map.Entry<String, Object> valueEntry : values.entrySet()) {
				processOneMetric(resultStrings, result, valueEntry.getValue(), tagName, valueEntry.getKey());
			}
		}
		return resultStrings;
	}

	/**
	 * Process a single metric from the given JMX query result with the specified value.
	 */
	protected void processOneMetric(List<String> resultStrings, Result result, Object value, String addTagName,
									String addTagValue) {
		String metricName = this.metricNameStrategy.formatName(result);

		//
		// Skip any non-numeric values since OpenTSDB only supports numeric metrics.
		//
		if (NumberUtils.isNumeric(value)) {
			StringBuilder resultString = new StringBuilder();

			formatResultString(resultString, metricName, result.getEpoch() / 1000L, value);
			addTags(resultString);

			if (addTagName != null) {
				addTag(resultString, addTagName, addTagValue);
			}

			if (getTypeNames().size() > 0) {
				this.addTypeNamesTags(resultString, result);
			}

			resultStrings.add(resultString.toString());
		} else {
			log.debug("Skipping non-numeric value for metric {}; value={}", metricName, value);
		}
	}

	/**
	 * Add the tag(s) for typeNames.
	 *
	 * @param result       - the result of the JMX query.
	 * @param resultString - current form of the metric string.
	 * @return String - the updated metric string with the necessary tag(s) added.
	 */
	protected void addTypeNamesTags(StringBuilder resultString, Result result) {
		if (mergeTypeNamesTags) {
			// Produce a single tag with all the TypeName keys concatenated and all the values joined with '_'.
			addTag(resultString, StringUtils.join(getTypeNames(), ""), getConcatedTypeNameValues(result.getTypeName()));
		} else {
			Map<String, String> typeNameMap = KeyUtils.getTypeNameValueMap(result.getTypeName());
			for (String oneTypeName : getTypeNames()) {
				String value = typeNameMap.get(oneTypeName);
				if (value == null)
					value = "";
				addTag(resultString, oneTypeName, value);
			}
		}
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
					System.out.println(resultString);

				this.sendOutput(resultString);
			}
		}
		this.finishOutput();
	}

	/**
	 * Validation per query, after the writer has been start()ed
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	/**
	 * Start the output writer.  At this time, the settings are read from the configuration file and saved for later
	 * use.
	 */
	@Override
	public void start() throws LifecycleException {
		this.prepareSender();
	}

	@Override
	public void stop() throws LifecycleException {
		this.shutdownSender();
	}

	/**
	 * VALID CHARACTERS:
	 * METRIC, TAGNAME, AND TAG-VALUE:
	 * [-_./a-zA-Z0-9]+
	 * <p/>
	 * <p/>
	 * SANITIZATION:
	 * - Discard Quotes.
	 * - Replace all other invalid characters with '_'.
	 */
	protected String sanitizeString(String unsanitized) {
		return unsanitized.
				replaceAll("[\"']", "").
				replaceAll("[^-_./a-zA-Z0-9]", "_");
	}
}
