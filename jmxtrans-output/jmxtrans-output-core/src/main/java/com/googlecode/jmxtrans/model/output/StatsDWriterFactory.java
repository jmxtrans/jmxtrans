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
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.UdpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;
import com.googlecode.jmxtrans.model.output.support.pool.FlushStrategy;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.jmxtrans.model.output.support.pool.FlushStrategyUtils.createFlushStrategy;

@EqualsAndHashCode
@ToString
public class StatsDWriterFactory implements OutputWriterFactory {

	@Nonnull private final ImmutableList<String> typeNames;
	@Nonnull private final String rootPrefix;
	private final boolean stringsValuesAsKey;
	@Nonnull private final String bucketType;
	@Nonnull private final Long stringValueDefaultCount;
	@Nonnull private final InetSocketAddress server;
	@Nonnull private final FlushStrategy flushStrategy;
	private final int poolSize;
	@Nonnull private final String replacementForInvalidChar;


	public StatsDWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("bucketType") String bucketType,
			@JsonProperty("stringValuesAsKey") boolean stringsValuesAsKey,
			@JsonProperty("stringValueDefaultCount") Long stringValueDefaultCount,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("flushStrategy") String flushStrategy,
			@JsonProperty("flushDelayInSeconds") Integer flushDelayInSeconds,
			@JsonProperty("poolSize") Integer poolSize,
			@JsonProperty("replacementForInvalidChar") String replacementForInvalidChar
	) {
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
		this.rootPrefix = firstNonNull(rootPrefix, "servers");
		this.stringsValuesAsKey = stringsValuesAsKey;
		this.bucketType = firstNonNull(bucketType, "c");
		this.stringValueDefaultCount = firstNonNull(stringValueDefaultCount, 1L);
		this.server = new InetSocketAddress(
				checkNotNull(host, "Host cannot be null."),
				checkNotNull(port, "Port cannot be null."));
		this.flushStrategy = createFlushStrategy(flushStrategy, flushDelayInSeconds);
		this.poolSize = firstNonNull(poolSize, 1);
		this.replacementForInvalidChar = firstNonNull(replacementForInvalidChar, "_");
	}

	@Override
	public WriterPoolOutputWriter<StatsDWriter2> create() {
		return UdpOutputWriterBuilder.builder(
				server,
				new StatsDWriter2(typeNames, rootPrefix, bucketType, stringsValuesAsKey, stringValueDefaultCount, replacementForInvalidChar))
				.setCharset(UTF_8)
				.setFlushStrategy(flushStrategy)
				.setPoolSize(poolSize)
				.build();
	}
}
