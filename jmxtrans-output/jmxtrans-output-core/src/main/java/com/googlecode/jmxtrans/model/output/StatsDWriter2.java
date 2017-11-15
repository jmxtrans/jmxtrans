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

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import com.googlecode.jmxtrans.model.results.CPrecisionValueTransformer;
import com.googlecode.jmxtrans.model.results.ValueTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

public class StatsDWriter2 implements WriterBasedOutputWriter {

	@Nonnull
	private static final Logger log = LoggerFactory.getLogger(StatsDWriter2.class);

	@Nonnull
	private final List<String> typeNames;
	@Nonnull
	private final String rootPrefix;
	private final boolean stringsValuesAsKey;
	@Nonnull
	private final String bucketType;
	@Nonnull
	private final String stringValueDefaultCount;
	@Nonnull
	private final String replacementForInvalidChar;

	@Nonnull
	private final ValueTransformer valueTransformer = new CPrecisionValueTransformer();

	private static final Pattern STATSD_INVALID = Pattern.compile("[:|]");

	public StatsDWriter2(
			@Nonnull List<String> typeNames,
			@Nonnull String rootPrefix,
			@Nonnull String bucketType,
			boolean stringsValuesAsKey,
			@Nonnull Long stringValueDefaultCount,
			@Nonnull String replacementForInvalidChar) {
		this.typeNames = typeNames;
		this.rootPrefix = rootPrefix;
		this.stringsValuesAsKey = stringsValuesAsKey;
		this.bucketType = bucketType;
		this.stringValueDefaultCount = stringValueDefaultCount.toString();
		this.replacementForInvalidChar = replacementForInvalidChar;

	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {
		for (Result result : results) {
			String key = KeyUtils.getKeyString(server, query, result, typeNames, rootPrefix);
			if (isNotValidValue(result.getValue())) {
				log.debug("Skipping message key[{}] with value: {}.", key, result.getValue());
				continue;
			}

			// These characters can mess with formatting.
			String line = STATSD_INVALID.matcher(key).replaceAll(replacementForInvalidChar)
				+ computeActualValue(result.getValue()) + "|" + bucketType + "\n";

			writer.write(line);
		}
	}

	private boolean isNotValidValue(Object value) {
		return !(isNumeric(value) || stringsValuesAsKey);
	}

	private String computeActualValue(Object value) {
		Object transformedValue = valueTransformer.apply(value);
		if (isNumeric(transformedValue)) {
			return ":" + transformedValue.toString();
		} else if (transformedValue == null) {
			return ":";
		}

		return "." + transformedValue.toString() + ":" + stringValueDefaultCount;
	}
}
