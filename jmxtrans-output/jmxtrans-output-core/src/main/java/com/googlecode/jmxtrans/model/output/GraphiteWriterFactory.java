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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.model.output.support.TcpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.UdpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;
import com.googlecode.jmxtrans.model.output.support.pool.FlushStrategy;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.net.InetSocketAddress;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.jmxtrans.model.output.support.pool.FlushStrategyUtils.createFlushStrategy;

/**
 * This low latency and thread safe output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">Getting your data into Graphite</a>
 */
@ThreadSafe
@EqualsAndHashCode
@ToString
public class GraphiteWriterFactory implements OutputWriterFactory {

	private static final String DEFAULT_ROOT_PREFIX = "servers";
	private static final String DEFAULT_PROTOCOL = "tcp";

	@Nonnull private final String rootPrefix;
	@Nonnull private final InetSocketAddress graphiteServer;
	@Nonnull private final ImmutableList<String> typeNames;
	private final boolean booleanAsNumber;
	@Nonnull private final FlushStrategy flushStrategy;
	private final int poolSize;
	private final int socketTimeoutMs;
	private final Integer poolClaimTimeoutSeconds;

	/**
	 * protocol to use to send metrics to graphite server.
	 * Default to "tcp". Possible values: "udp" or omit the value to use tcp protocol.
	 */
	private final String protocol;

	@JsonCreator
	public GraphiteWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("flushStrategy") String flushStrategy,
			@JsonProperty("flushDelayInSeconds") Integer flushDelayInSeconds,
			@JsonProperty("poolSize") Integer poolSize,
			@JsonProperty("socketTimeoutMs") Integer socketTimeoutMs,
			@JsonProperty("poolClaimTimeoutSeconds") Integer poolClaimTimeoutSeconds,
			@JsonProperty("protocol") String protocol) {

		this.typeNames = typeNames;
		this.booleanAsNumber = booleanAsNumber;
		this.rootPrefix = firstNonNull(rootPrefix, DEFAULT_ROOT_PREFIX);

		this.graphiteServer = new InetSocketAddress(
				checkNotNull(host, "Host cannot be null."),
				checkNotNull(port, "Port cannot be null."));
		this.flushStrategy = createFlushStrategy(flushStrategy, flushDelayInSeconds);
		this.poolSize = firstNonNull(poolSize, 1);
		this.socketTimeoutMs = firstNonNull(socketTimeoutMs, 200);
		this.poolClaimTimeoutSeconds = firstNonNull(poolClaimTimeoutSeconds, 1);
		this.protocol = firstNonNull(protocol, DEFAULT_PROTOCOL);
	}

	@Override
	public ResultTransformerOutputWriter<WriterPoolOutputWriter<GraphiteWriter2>> create() {

		WriterPoolOutputWriter<GraphiteWriter2> writerPoolOutputWriter;
		// check if we want to use udp protocol or fallback on default tcp protocol
		if ("udp".equals(this.protocol)) {
			writerPoolOutputWriter = UdpOutputWriterBuilder.builder(graphiteServer, new GraphiteWriter2(typeNames, rootPrefix))
					.setCharset(UTF_8)
					.setFlushStrategy(flushStrategy)
					.setPoolSize(poolSize)
					.setPoolClaimTimeoutSeconds(poolClaimTimeoutSeconds)
					.build();
		} else {
			writerPoolOutputWriter = TcpOutputWriterBuilder.builder(graphiteServer, new GraphiteWriter2(typeNames, rootPrefix))
					.setCharset(UTF_8)
					.setFlushStrategy(flushStrategy)
					.setPoolSize(poolSize)
					.setSocketTimeoutMillis(socketTimeoutMs)
					.setPoolClaimTimeoutSeconds(poolClaimTimeoutSeconds)
					.build();

		}

		return ResultTransformerOutputWriter.booleanToNumber(booleanAsNumber, writerPoolOutputWriter);

	}

}
