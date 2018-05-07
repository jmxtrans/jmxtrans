/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
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
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Map;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * @author Qiannan Lu
 */
@EqualsAndHashCode(exclude = {"jsonFactory"})
public class OpenFalconWriter implements WriterBasedOutputWriter {
    private static final String ORIGIN = "GAUGE";
    private static final String DELTA_PS = "COUNTER";
    private static final String DELTA = "";

    @Nonnull
    private final JsonFactory jsonFactory;
    @Nonnull
    private final ImmutableList<String> typeNames;

    private String endpoint;
    private String tags;

    public OpenFalconWriter(@Nonnull JsonFactory jsonFactory, @Nonnull ImmutableList<String> typeNames, String endpoint, String tags) {
        this.jsonFactory = jsonFactory;
        this.typeNames = typeNames;
        this.endpoint = endpoint;
        this.tags = tags;
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
                        g.writeStringField("metric", KeyUtils.getKeyString(query, result, values, typeNames));
//                        String hostname = InetAddress.getLocalHost().getHostName();
                        g.writeStringField("endpoint", StringUtils.isNotBlank(endpoint) ? endpoint : InetAddress.getLocalHost().getHostName() + ".jvm");
                        g.writeStringField("counterType", ORIGIN);
                        g.writeNumberField("timestamp", System.currentTimeMillis() / 1000L);
                        g.writeNumberField("step", 60);
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
                            g.writeStringField("value", value.toString());
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