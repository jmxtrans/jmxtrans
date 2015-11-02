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
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.model.output.support.TcpOutputWriter;

import javax.annotation.concurrent.ThreadSafe;
import java.net.InetSocketAddress;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This low latency and thread safe output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">Getting your data into Graphite</a>
 */
@ThreadSafe
public class GraphiteWriterFactory implements OutputWriterFactory {

	private static final String DEFAULT_ROOT_PREFIX = "servers";

	private final String rootPrefix;
	private final InetSocketAddress graphiteServer;
	private final ImmutableList<String> typeNames;
	private final boolean booleanAsNumber;

	@JsonCreator
	public GraphiteWriterFactory(
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
				TcpOutputWriter.builder(graphiteServer, new GraphiteWriter2(typeNames, rootPrefix))
						.setCharset(UTF_8)
						.build()
		);
	}

}
