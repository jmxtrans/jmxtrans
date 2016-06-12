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
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class SensuWriter2 implements WriterBasedOutputWriter {

	@Nonnull private final GraphiteWriter2 graphiteWriter;
	@Nonnull private final JsonFactory jsonFactory;

	public SensuWriter2(@Nonnull GraphiteWriter2 graphiteWriter, @Nonnull JsonFactory jsonFactory) {
		this.graphiteWriter = graphiteWriter;
		this.jsonFactory = jsonFactory;
	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {
		try (
				JsonGenerator g = jsonFactory.createGenerator(writer);
				StringWriter temporaryWriter = new StringWriter()
		) {
			g.useDefaultPrettyPrinter();
			g.writeStartObject();
			g.writeStringField("name", "jmxtrans");
			g.writeStringField("type", "metric");
			g.writeStringField("handler", "graphite");

			graphiteWriter.write(temporaryWriter, server, query, results);

			g.writeStringField("output", temporaryWriter.toString());
			g.writeEndObject();
			g.flush();
		}
	}
}
