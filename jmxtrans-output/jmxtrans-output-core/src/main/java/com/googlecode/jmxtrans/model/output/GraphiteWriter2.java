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

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import com.googlecode.jmxtrans.util.OnlyOnceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@ThreadSafe
public class GraphiteWriter2 implements WriterBasedOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(GraphiteWriter2.class);
	private final OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(log);

	@Nonnull private final ImmutableList<String> typeNames;
	@Nullable private final String rootPrefix;

	public GraphiteWriter2(@Nonnull ImmutableList<String> typeNames, @Nullable String rootPrefix) {
		this.typeNames = typeNames;
		this.rootPrefix = rootPrefix;
	}

	@Override
	public void write(
			@Nonnull Writer writer,
			@Nonnull Server server,
			@Nonnull Query query,
			@Nonnull Iterable<Result> results) throws IOException {

		for (Result result : results) {
			log.debug("Query result: {}", result);
			Map<String, Object> resultValues = result.getValues();
			for (Map.Entry<String, Object> values : resultValues.entrySet()) {
				Object value = values.getValue();
				if (isNumeric(value)) {

					String line = KeyUtils.getKeyString(server, query, result, values, typeNames, rootPrefix)
							.replaceAll("[()]", "_") + " " + value.toString() + " "
							+ SECONDS.convert(result.getEpoch(), MILLISECONDS) + "\n";
					log.debug("Graphite Message: {}", line);
					writer.write(line);
				} else {
					onlyOnceLogger.infoOnce("Unable to submit non-numeric value to Graphite: [{}] from result [{}]", value, result);
				}
			}
		}
	}
}
