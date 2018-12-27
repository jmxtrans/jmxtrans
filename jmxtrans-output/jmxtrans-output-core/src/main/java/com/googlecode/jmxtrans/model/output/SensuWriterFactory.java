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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.model.output.support.TcpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;
import com.googlecode.jmxtrans.model.output.support.pool.FlushStrategy;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.googlecode.jmxtrans.model.output.support.pool.FlushStrategyUtils.createFlushStrategy;

@EqualsAndHashCode
@ToString
public class SensuWriterFactory implements OutputWriterFactory {

	private final boolean booleanAsNumber;
	@Nonnull private final InetSocketAddress server;
	@Nonnull private final ImmutableList<String> typeNames;
	@Nullable private final String rootPrefix;
	@Nonnull private final String handler;
	@Nonnull private final String name;
	@Nonnull private final String type;
	@Nonnull private final FlushStrategy flushStrategy;
	private final int poolSize;

	public SensuWriterFactory(
		@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
		@JsonProperty("flushDelayInSeconds") Integer flushDelayInSeconds,
		@JsonProperty("flushStrategy") String flushStrategy,
		@JsonProperty("handler") String handler,
		@JsonProperty("host") String host,
		@JsonProperty("name") String name,
		@JsonProperty("port") Integer port,
		@JsonProperty("rootPrefix") String rootPrefix,
		@JsonProperty("type") String type,
		@JsonProperty("typeNames") ImmutableList<String> typeNames,
		@JsonProperty("poolSize") Integer poolSize
	) {
		this.booleanAsNumber = booleanAsNumber;
		this.flushStrategy = createFlushStrategy(flushStrategy, flushDelayInSeconds);
		this.handler = firstNonNull(handler, "graphite");
		this.name = firstNonNull(name, "metrics-jmxtrans");
		this.poolSize = firstNonNull(poolSize, 1);
		this.rootPrefix = rootPrefix;
		this.server = new InetSocketAddress(
			firstNonNull(host, "localhost"),
			firstNonNull(port, 3030)
		);
		this.type = firstNonNull(type, "metric");
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
	}

	@Override
	public ResultTransformerOutputWriter<WriterPoolOutputWriter<SensuWriter2>> create() {
		return ResultTransformerOutputWriter.booleanToNumber(
			booleanAsNumber,
			TcpOutputWriterBuilder.builder(
				server,
				new SensuWriter2(
					new GraphiteWriter2(typeNames, rootPrefix),
					new JsonFactory(),
					handler,
					name,
					type
				)
			)
			.setFlushStrategy(flushStrategy)
			.setPoolSize(poolSize)
			.build()
		);
	}
}
