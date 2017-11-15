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
package com.googlecode.jmxtrans.model.output.gelf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode
public class GelfWriter extends OutputWriterAdapter {

	private static final Logger log = LoggerFactory.getLogger(GelfWriter.class);

	private final Map<String, Object> additionalFields;
	private final GelfTransport gelfTransport;
	private final ImmutableList<String> typeNames;

	public GelfWriter(
		final ImmutableList<String> typeNames,
		final Map<String, Object> additionalFields,
		final GelfTransport gelfTransport
	) {
		this.typeNames = typeNames;
		this.additionalFields = additionalFields;
		this.gelfTransport = gelfTransport;
	}

	@Override
	public void doWrite(final Server server, final Query query, final Iterable<Result> results)
		throws Exception
	{
		final GelfMessageBuilder messageBuilder = new GelfMessageBuilder(
			"",
			server.getHost()
		);

		final List<String> messages = new ArrayList<>();

		for (final Result result : results) {
			log.debug("Query result: [{}]", result);
			final String
				key =
				KeyUtils.getKeyString(query, result, this.typeNames);

			messages.add(
				String.format(
					"%s=%s",
					key,
					result.getValue()
				)
			);

			messageBuilder.additionalField(key, result.getValue());
		}

		if (additionalFields != null) {
			for (final Map.Entry<String, Object> additionalField : additionalFields
				.entrySet()) {
				messageBuilder.additionalField(additionalField.getKey(),
					additionalField.getValue());
			}
		}

		final String message = StringUtils.join(messages, " ");

		messageBuilder.message(message);
		messageBuilder.fullMessage(message);

		log.debug(
			"Sending GELF message: {}",
			messageBuilder.build().toString()
		);

		this.gelfTransport.send(messageBuilder.build());
	}

	@VisibleForTesting
	public Map<String, Object> getAdditionalFields() {
		return additionalFields;
	}
}
