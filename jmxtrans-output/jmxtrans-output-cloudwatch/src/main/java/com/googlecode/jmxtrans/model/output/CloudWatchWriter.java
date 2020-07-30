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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Kept for backward compatibility with the older json files having output writer mentioned as CloudWatchWriter.
 *
 * @deprecated use {@link CloudWatchWriterFactory} instead.
 */
@Deprecated
public class CloudWatchWriter implements OutputWriterFactory {

	@Nonnull
	private final CloudWatchWriterFactory cloudWatchWriterFactory;


	@JsonCreator
	public CloudWatchWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("namespace") String namespace,
			@JsonProperty("dimensions") Collection<Map<String, Object>> dimensions,
			@JsonProperty("settings") Map<String, Object> settings) {
		this.cloudWatchWriterFactory = new CloudWatchWriterFactory(
				typeNames,
				booleanAsNumber,
				debugEnabled,
				firstNonNull(namespace, (String) settings.get("namespace")),
				firstNonNull(dimensions, (Collection<Map<String, Object>>) settings.get("dimensions"))
		);
	}

	@Nonnull
	@Override
	public ResultTransformerOutputWriter<CloudWatchWriter2> create() {
		return this.cloudWatchWriterFactory.create();
	}

	@VisibleForTesting
	@Nonnull
	CloudWatchWriterFactory getCloudWatchWriterFactory() {
		return cloudWatchWriterFactory;
	}
}
