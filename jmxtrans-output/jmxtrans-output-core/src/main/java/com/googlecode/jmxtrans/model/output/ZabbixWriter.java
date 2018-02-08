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
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.Writer;

@ThreadSafe
public class ZabbixWriter implements WriterBasedOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(ZabbixWriter.class);

	@Nonnull private final JsonFactory jsonFactory;
	@Nonnull private final ImmutableList<String> typeNames;

	public ZabbixWriter(@Nonnull JsonFactory jsonFactory, @Nonnull ImmutableList<String> typeNames) {
		this.jsonFactory = jsonFactory;
		this.typeNames = typeNames;
	}

	@Override
	public void write(
			@Nonnull Writer writer,
			@Nonnull Server server,
			@Nonnull Query query,
			@Nonnull Iterable<Result> results) throws IOException {

		/* Zabbix Sender JSON
		{
			"request":"sender data",
			"data":[
				{ "host":"Host name 1", "key":"item_key", "value":"33", "clock": 1381482894 },
				{ "host":"Host name 2", "key":"item_key", "value":"55", "clock": 1381482894 }
			],
			"clock": 1381482905
		}
		*/
		try (
			JsonGenerator g = jsonFactory.createGenerator(writer);
		) {
			g.useDefaultPrettyPrinter();
			g.writeStartObject();
			g.writeStringField("request", "sender data");
			g.writeArrayFieldStart("data");

			for (Result result : results) {
				log.debug("Query result: {}", result);

				String key = "jmxtrans." + KeyUtils.getKeyString(query, result, typeNames);
				Object value = result.getValue();

				g.writeStartObject();
				g.writeStringField("host", server.getLabel());
				g.writeStringField("key", key);
				g.writeStringField("value", value.toString());
				g.writeNumberField("clock", result.getEpoch() / 1000);
				g.writeEndObject();
			}

			g.writeEndArray();
			g.writeNumberField("clock", System.currentTimeMillis() / 1000);
			g.writeEndObject();
			g.flush();
		}
	}
}
