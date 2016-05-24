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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * @author Qiannan Lu
 * Modifier: modeyang 2016/5/13
 */
public class OpenFalconWriter implements WriterBasedOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(OpenFalconWriter.class);
	private static final String ORIGIN = "GAUGE";
	private static final String DELTA_PS = "COUNTER";
	private static final Integer STEP = 60;

	@Nonnull
	private final JsonFactory jsonFactory;
	@Nonnull
	private final ImmutableList<String> typeNames;

	private final String endpoint;
	private final String tags;
	private final String metricType;

	public OpenFalconWriter(@Nonnull JsonFactory jsonFactory, @Nonnull ImmutableList<String> typeNames, String endpoint, String tags, String metricType) {
		this.jsonFactory = jsonFactory;
		this.typeNames = typeNames;
		this.endpoint = endpoint;
		this.tags = tags;
		if (metricType.length() == 0) {
			this.metricType = ORIGIN;
		} else {
			if (metricType.toUpperCase().equals(ORIGIN) || metricType.toUpperCase().equals(DELTA_PS)) {
				log.error("metricType must in [" + ORIGIN + "," + DELTA_PS + "]" + ", not " + metricType.toUpperCase());
				metricType = ORIGIN;
			}
			this.metricType = metricType;
		}
	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {
		Closer closer = Closer.create();
		try {
			JsonGenerator g = closer.register(jsonFactory.createGenerator(writer));
			g.writeStartArray();
			for (Result result : results) {
				Map<String, Object> resultValues = result.getValues();
				for (Map.Entry<String, Object> values : resultValues.entrySet()) {
					if (isNumeric(values.getValue())) {
						g.writeStartObject();
						g.writeStringField("metric", result.getKeyAlias() + "." + result.getAttributeName());
						String tags = this.tags.trim();
						if (tags.length() > 0) {
							tags += ",";
						}
						tags += "port=" + server.getPort().toString() + "," + result.getTypeName();
						g.writeStringField("tags", tags);
						g.writeStringField("endpoint", server.getHost());
						g.writeStringField("counterType", this.metricType.toUpperCase());
						g.writeNumberField("timestamp", System.currentTimeMillis() / 1000L);
						g.writeNumberField("step", STEP);
						Object value = values.getValue();
						if (value instanceof Integer) {
							g.writeNumberField("value", (Integer) value);
						} else if (value instanceof Long) {
							g.writeNumberField("value", (Long) value);
						} else if (value instanceof Float) {
							g.writeNumberField("value", (Float) value);
						} else if (value instanceof Double) {
							g.writeNumberField("value", (Double) value);
						} else {
							// g.writeStringField("value", value.toString());
							log.warn("open falcon current not support string field value");
						}
						g.writeEndObject();
					}
				}
			}
			g.writeEndArray();
			g.flush();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}
}
