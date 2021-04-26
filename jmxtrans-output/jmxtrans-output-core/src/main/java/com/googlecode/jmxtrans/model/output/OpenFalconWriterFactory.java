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
import com.googlecode.jmxtrans.model.output.support.HttpOutputWriter;
import com.googlecode.jmxtrans.model.output.support.HttpUrlConnectionConfigurer;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;

import javax.annotation.Nonnull;
import java.net.URL;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @date  2016/4/5.
 * @author  Qiannan Lu
 * Modifier: modeyang 2016/5/13
 */
public class OpenFalconWriterFactory implements OutputWriterFactory {
	private final boolean booleanAsNumber;
	@Nonnull
	private final ImmutableList<String> typeNames;
	@Nonnull
	private final URL url;

	private final int readTimeoutInMillis;

	private final String endpoint;
	private final String tags;
	private final String metricType;

	public OpenFalconWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("url") URL url,
			@JsonProperty("readTimeoutInMillis") Integer readTimeoutInMillis,
			@JsonProperty("endpoint") String endpoint,
			@JsonProperty("tags") String tags,
			@JsonProperty("metricType") String metricType) {
		this.booleanAsNumber = booleanAsNumber;
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
		this.url = checkNotNull(url);
		this.readTimeoutInMillis = firstNonNull(readTimeoutInMillis, 0);
		this.endpoint = endpoint;
		this.tags = tags;
		this.metricType = metricType;
	}

	@Override
	@Nonnull
	public ResultTransformerOutputWriter<HttpOutputWriter<OpenFalconWriter>> create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				new HttpOutputWriter<>(
						new OpenFalconWriter(
								new JsonFactory(),
								typeNames,
								endpoint,
								tags),
						url,
						null,
						new HttpUrlConnectionConfigurer(
								"POST",
								readTimeoutInMillis,
								null,
								"application/json; charset=utf-8"
						),
						UTF_8
				));
	}
}
