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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.model.output.support.TcpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;

import static com.google.common.base.MoreObjects.firstNonNull;

public class SensuWriterFactory implements OutputWriterFactory {

	private final boolean booleanAsNumber;
	@Nonnull private final InetSocketAddress server;
	@Nonnull private final ImmutableList<String> typeNames;
	@Nullable private final String rootPrefix;

	public SensuWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("rootPrefix") String rootPrefix) {
		this.rootPrefix = rootPrefix;
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
		this.booleanAsNumber = booleanAsNumber;
		this.server = new InetSocketAddress(
				firstNonNull(host, "localhost"),
				firstNonNull(port, 3030));
	}

	@Override
	public ResultTransformerOutputWriter<WriterPoolOutputWriter<SensuWriter2>> create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				TcpOutputWriterBuilder.builder(
						server,
						new SensuWriter2(
								new GraphiteWriter2(typeNames, rootPrefix),
								new JsonFactory()))
						.build());
	}

}
