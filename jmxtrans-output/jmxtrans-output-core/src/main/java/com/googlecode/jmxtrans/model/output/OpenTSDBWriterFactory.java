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
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.model.output.support.TcpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;
import com.googlecode.jmxtrans.model.output.support.opentsdb.OpenTSDBMessageFormatter;
import com.googlecode.jmxtrans.model.output.support.pool.FlushStrategy;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.googlecode.jmxtrans.model.output.support.pool.FlushStrategyUtils.createFlushStrategy;

@ThreadSafe
@EqualsAndHashCode
@ToString
public class OpenTSDBWriterFactory implements OutputWriterFactory {

	@Nonnull private final boolean booleanAsNumber;
	@Nonnull private final InetSocketAddress server;
	@Nonnull private final OpenTSDBMessageFormatter messageFormatter;
	@Nonnull private final FlushStrategy flushStrategy;
	private final int poolSize;

	@JsonCreator
	public OpenTSDBWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("tags") Map<String, String> tags,
			@JsonProperty("tagName") String tagName,
			@JsonProperty("mergeTypeNamesTags") Boolean mergeTypeNamesTags,
			@JsonProperty("metricNamingExpression") String metricNamingExpression,
			@JsonProperty("addHostnameTag") Boolean addHostnameTag,
			@JsonProperty("flushStrategy") String flushStrategy,
			@JsonProperty("flushDelayInSeconds") Integer flushDelayInSeconds,
			@JsonProperty("poolSize") Integer poolSize) throws LifecycleException, UnknownHostException {

		this.booleanAsNumber = booleanAsNumber;
		this.server = new InetSocketAddress(
				firstNonNull(host, "localhost"),
				firstNonNull(port, 3030));

		ImmutableMap<String, String> immutableTags =
				tags == null ? ImmutableMap.<String, String>of() : ImmutableMap.copyOf(tags);

		mergeTypeNamesTags = firstNonNull(mergeTypeNamesTags, true);

		messageFormatter = new OpenTSDBMessageFormatter(
				(typeNames == null) ? ImmutableList.<String>of() : typeNames,
				immutableTags, tagName, metricNamingExpression, mergeTypeNamesTags,
				addHostnameTag ? InetAddress.getLocalHost().getHostName() : null);
		this.flushStrategy = createFlushStrategy(flushStrategy, flushDelayInSeconds);
		this.poolSize = firstNonNull(poolSize, 1);
	}

	@Override
	public ResultTransformerOutputWriter<WriterPoolOutputWriter<OpenTSDBWriter2>> create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				TcpOutputWriterBuilder
						.builder(server, new OpenTSDBWriter2(messageFormatter))
						.setFlushStrategy(flushStrategy)
						.setPoolSize(poolSize)
						.build()
		);
	}
}
