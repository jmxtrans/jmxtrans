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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.model.output.support.TcpOutputWriter;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import com.googlecode.jmxtrans.util.OnlyOnceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * This low latency and thread safe output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">Getting your data into Graphite</a>
 */
@ThreadSafe
public class GraphiteWriter2 implements OutputWriterFactory {
	private static final Logger log = LoggerFactory.getLogger(GraphiteWriter2.class);

	private static final String DEFAULT_ROOT_PREFIX = "servers";

	private final String rootPrefix;
	private final InetSocketAddress graphiteServer;
	private final ImmutableList<String> typeNames;
	private final boolean booleanAsNumber;

	@JsonCreator
	public GraphiteWriter2(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port) {
		this.typeNames = typeNames;
		this.booleanAsNumber = booleanAsNumber;
		this.rootPrefix = firstNonNull(rootPrefix, DEFAULT_ROOT_PREFIX);

		this.graphiteServer = new InetSocketAddress(
				checkNotNull(host, "Host cannot be null."),
				checkNotNull(port, "Port cannot be null."));
	}

	@Override
	public OutputWriter create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				TcpOutputWriter.builder(graphiteServer, new W(typeNames, rootPrefix))
						.setCharset(UTF_8)
						.build()
		);
	}

	@ThreadSafe
	public static class W implements WriterBasedOutputWriter {
		private final OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(log);

		private final ImmutableList<String> typeNames;
		private final String rootPrefix;

		public W(ImmutableList<String> typeNames, String rootPrefix) {
			this.typeNames = typeNames;
			this.rootPrefix = rootPrefix;
		}

		@Override
		public void write(
				@Nonnull Writer writer,
				@Nonnull Server server,
				@Nonnull Query query,
				@Nonnull ImmutableList<Result> results) throws IOException {

			for (Result result : results) {
				log.debug("Query result: {}", result);
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						Object value = values.getValue();
						if (isNumeric(value)) {

							String line = KeyUtils.getKeyString(server, query, result, values, typeNames, rootPrefix)
									.replaceAll("[()]", "_") + " " + value.toString() + " "
									+ result.getEpoch() / 1000 + "\n";
							log.debug("Graphite Message: {}", line);
							writer.write(line);
						} else {
							onlyOnceLogger.infoOnce("Unable to submit non-numeric value to Graphite: [{}] from result [{}]", value, result);
						}
					}
				}
			}
		}
	}

}
