/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
package com.googlecode.jmxtrans.model.output.support.opentsdb;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.NamingStrategy;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.ClassAttributeNamingStrategy;
import com.googlecode.jmxtrans.model.naming.JexlNamingStrategy;
import com.googlecode.jmxtrans.model.naming.typename.TypeNameValue;
import com.googlecode.jmxtrans.model.naming.typename.TypeNameValuesStringBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

@EqualsAndHashCode
@ToString
public class OpenTSDBMessageFormatter {

	private static final Logger log = LoggerFactory.getLogger(OpenTSDBMessageFormatter.class);
	public static final String DEFAULT_TAG_NAME = "type";

	private final ImmutableList<String> typeNames;
	private final ImmutableMap<String, String> tags;
	private final String tagName;
	private final NamingStrategy metricNameStrategy;


	private final boolean mergeTypeNamesTags;
	private final boolean hostnameTag;

	public OpenTSDBMessageFormatter(@Nonnull ImmutableList<String> typeNames,
									@Nonnull ImmutableMap<String, String> tags) throws LifecycleException {
		this(typeNames, tags, DEFAULT_TAG_NAME, null, true, true);
	}

	public OpenTSDBMessageFormatter(@Nonnull ImmutableList<String> typeNames,
									@Nonnull ImmutableMap<String, String> tags,
									@Nonnull String tagName,
									@Nullable String metricNamingExpression,
									boolean mergeTypeNamesTags,
									boolean hostnameTag) throws LifecycleException {
		this.typeNames = typeNames;
		this.tags = tags;
		this.tagName = tagName;
		if (metricNamingExpression != null) {
			try {
				metricNameStrategy = new JexlNamingStrategy(metricNamingExpression);
			} catch (JexlException jexlExc) {
				throw new LifecycleException("failed to setup naming strategy", jexlExc);
			}
		} else {
			metricNameStrategy = new ClassAttributeNamingStrategy();
		}
		this.mergeTypeNamesTags = mergeTypeNamesTags;
		this.hostnameTag = hostnameTag;
	}


	/**
	 * Add tags to the given result string, including a "host" tag with the name of the server and all of the tags
	 * defined in the "settings" entry in the configuration file within the "tag" element.
	 *
	 * @param resultString - the string containing the metric name, timestamp, value, and possibly other content.
	 */
	void addTags(StringBuilder resultString, Server server) {
		if (hostnameTag) {
			addTag(resultString, "host", server.getLabel());
		}

		// Add the constant tag names and values.
		for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
			addTag(resultString, tagEntry.getKey(), tagEntry.getValue());
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
	private void formatResultString(StringBuilder resultString, String metricName, long epoch, Object value) {
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
	 * @param server - Server object for importing hostname
	 * @return List<String> - the list of strings containing metric details ready for sending to OpenTSDB.
	 */
	/*
	private List<String> formatResult(Result result) {
		return this.formatResult(result, null);
	}
	*/
	private List<String> formatResult(Result result, Server server) {
		List<String> resultStrings = new LinkedList<>();

		if (result.getValuePath().isEmpty()) {
			processOneMetric(resultStrings, server, result, result.getValue(), null, null);
		} else {
			processOneMetric(resultStrings, server, result, result.getValue(), tagName, StringUtils.join(result.getValuePath(), '.'));
		}
		return resultStrings;
	}

	public Iterable<String> formatResults(Iterable<Result> results, final Server server) {
		return from(results).transformAndConcat(new Function<Result, List<String>>() {

			@Override
			public List<String> apply(Result input) {
				return formatResult(input, server);
			}

		}).toList();
	}

	/**
	 * Process a single metric from the given JMX query result with the specified value.
	 */
	protected void processOneMetric(List<String> resultStrings, Server server, Result result, Object value, String addTagName,
									String addTagValue) {
		String metricName = this.metricNameStrategy.formatName(result);

		//
		// Skip any non-numeric values since OpenTSDB only supports numeric metrics.
		//
		if (isNumeric(value)) {
			StringBuilder resultString = new StringBuilder();

			formatResultString(resultString, metricName, result.getEpoch() / 1000L, value);
			addTags(resultString, server);

			if (addTagName != null) {
				addTag(resultString, addTagName, addTagValue);
			}

			if (!typeNames.isEmpty()) {
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
			String typeNameValues = TypeNameValuesStringBuilder.getDefaultBuilder().build(typeNames, result.getTypeName());
			addTag(resultString, StringUtils.join(typeNames, ""), typeNameValues);
		} else {
			Map<String, String> typeNameMap = TypeNameValue.extractMap(result.getTypeName());
			for (String oneTypeName : typeNames) {
				String value = typeNameMap.get(oneTypeName);
				if (value == null)
					value = "";
				addTag(resultString, oneTypeName, value);
			}
		}
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
	protected String sanitizeString(String unSanitized) {
		return unSanitized.
				replaceAll("[\"']", "").
				replaceAll("[^-_./a-zA-Z0-9]", "_");
	}

}
