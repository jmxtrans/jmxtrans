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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import com.googlecode.jmxtrans.model.results.CPrecisionValueTransformer;
import com.googlecode.jmxtrans.model.results.ValueTransformer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

public class StatsDTelegrafWriter implements WriterBasedOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(StatsDTelegrafWriter.class);

	@Nonnull
	private final ValueTransformer valueTransformer = new CPrecisionValueTransformer();
	@Nonnull
	private final ImmutableList<String> bucketTypes;
	@Nonnull
	private final ImmutableMap<String, String> tags;
	@Nonnull
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;

	public StatsDTelegrafWriter(String[] bucketTypes, ImmutableMap<String, String> tags, ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags) {
		this.bucketTypes = ImmutableList.copyOf(bucketTypes);
		this.tags = tags;
		this.resultAttributesToWriteAsTags = resultAttributesToWriteAsTags;
	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {

		int resultIndex = -1;
		for (Result result : results) {
			resultIndex++;
			String bucketType = getBucketType(resultIndex);
			String attributeName = result.getAttributeName();

			List<String> resultTagList = new ArrayList<>();
			resultTagList.add(",jmxport=" + server.getPort());
			//tagList.add("objectName=" + query.getObjectName());
			resultTagList.add("attribute=" + attributeName);

			if (isNotValidValue(result.getValue())) {
				log.debug("Skipping message key[{}] with value: {}.", result.getAttributeName(), result.getValue());
				continue;
			}
			List<String> tagList = new ArrayList(resultTagList);

			if( !result.getValuePath().isEmpty() ){
				tagList.add("resultKey="+ KeyUtils.getValuePathString(result));
			}

			for (Map.Entry e : tags.entrySet()) {
				tagList.add(e.getKey() + "=" + e.getValue());
			}

			Number actualValue = computeActualValue(result.getValue());
			StringBuilder sb = new StringBuilder(result.getKeyAlias())
				.append(StringUtils.join(tagList, ","))
				.append(":").append(actualValue)
				.append("|").append(bucketType).append("\n");

			String output = sb.toString();
			if( actualValue.floatValue() < 0 && !StatsDMetricType.GAUGE.getKey().equals(bucketType) )
			{
				log.debug("Negative values are only supported for gauges, not sending: {}.", output);
			}
			else{
				log.debug(output);
				writer.write(output);
			}
		}
	}

	private String getBucketType(int resultIndex) {
		if( this.bucketTypes.size() > resultIndex ){
			return bucketTypes.get(resultIndex);
		}
		return Iterables.getLast(bucketTypes);
	}

	private boolean isNotValidValue(Object value) {
		return !isNumeric(value);
	}

	private Number computeActualValue(Object value) {
		Object transformedValue = valueTransformer.apply(value);
		if (isNumeric(transformedValue)) {
			return transformedValue instanceof Number ?
				(Number) transformedValue : Float.valueOf(transformedValue.toString());
		}
		throw new IllegalArgumentException("Only numeric values are supported, enable debug log level for more details.");
	}

}
