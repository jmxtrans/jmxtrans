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
package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;

import static com.googlecode.jmxtrans.model.naming.StringUtils.cleanupStr;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class LibratoWriter2 implements WriterBasedOutputWriter {

	@Nonnull private final JsonFactory jsonFactory;
	@Nonnull private final ImmutableList<String> typeNames;

	public LibratoWriter2(@Nonnull JsonFactory jsonFactory, @Nonnull ImmutableList<String> typeNames) {
		this.jsonFactory = jsonFactory;
		this.typeNames = typeNames;
	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {
		Closer closer = Closer.create();
		try {
			JsonGenerator g = closer.register(jsonFactory.createGenerator(writer));
			g.writeStartObject();
			g.writeArrayFieldStart("counters");
			g.writeEndArray();

			String source = getSource(server);

			g.writeArrayFieldStart("gauges");
			for (Result result : results) {
				if (isNumeric(result.getValue())) {
					g.writeStartObject();
					g.writeStringField("name", KeyUtils.getKeyString(query, result, typeNames));
					if (source != null && !source.isEmpty()) {
						g.writeStringField("source", source);
					}
					g.writeNumberField("measure_time", SECONDS.convert(result.getEpoch(), MILLISECONDS));
					Object value = result.getValue();
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
			g.writeEndArray();
			g.writeEndObject();
			g.flush();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	private String getSource(Server server) {
		if (server.getAlias() != null) {
			return server.getAlias();
		} else {
			return cleanupStr(server.getHost());
		}
	}

}
