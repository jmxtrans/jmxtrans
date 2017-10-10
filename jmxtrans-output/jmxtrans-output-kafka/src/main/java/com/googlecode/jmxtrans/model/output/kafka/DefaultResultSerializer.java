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
package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.ResultAttributes;
import com.googlecode.jmxtrans.model.Server;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
import static com.googlecode.jmxtrans.model.naming.KeyUtils.getKeyString;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * Original Result serializer (looks like OpenTSDB data format)
 */
@EqualsAndHashCode(exclude = {"jsonFactory"})
public class DefaultResultSerializer implements ResultSerializer {
	private static final Logger log = LoggerFactory.getLogger(DefaultResultSerializer.class);
	private final JsonFactory jsonFactory;
	@Getter @Nonnull
	private final ImmutableList<String> typeNames;
	@Getter
	private final boolean booleanAsNumber;
	@Getter @Nonnull
	private final String rootPrefix;
	@Getter @Nonnull
	private final ImmutableMap<String, String> tags;
	@Nonnull
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;

	@JsonCreator
	public DefaultResultSerializer(@JsonProperty("typeNames") List<String> typeNames,
									@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
									@JsonProperty("rootPrefix") String rootPrefix,
									@JsonProperty("tags") Map<String, String> tags,
									@JsonProperty("resultTags") List<String> resultTags
									) {
		this.jsonFactory = new JsonFactory();
		this.typeNames = typeNames == null ? ImmutableList.<String>of() : ImmutableList.copyOf(typeNames);
		this.booleanAsNumber = booleanAsNumber;
		this.rootPrefix = rootPrefix;
		this.tags = tags == null ? ImmutableMap.<String, String>of() : ImmutableMap.copyOf(tags);
		this.resultAttributesToWriteAsTags = resultTags == null ? ImmutableSet.<ResultAttribute>of() : ResultAttributes.forNames(resultTags);
	}

	@Nonnull
	@Override
	public Collection<String> serialize(Server server, Query query, Result result) throws IOException {
		log.debug("Query result: [{}]", result);
		Object value = result.getValue();
		if (isNumeric(value)) {
			return Collections.singleton(createJsonMessage(server, query, result, result.getValuePath(), value));
		} else {
			log.warn("Unable to submit non-numeric value to Kafka: [{}] from result [{}]", value, result);
			return Collections.emptyList();
		}
	}

	private String createJsonMessage(Server server, Query query, Result result, List<String> valuePath, Object value) throws IOException {
		String keyString = getKeyString(server, query, result, typeNames, this.rootPrefix);
		String cleanKeyString = keyString.replaceAll("[()]", "_");

		try (
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JsonGenerator generator = jsonFactory.createGenerator(out, UTF8)
		) {
			generator.writeStartObject();
			generator.writeStringField("keyspace", cleanKeyString);
			generator.writeStringField("value", value.toString());
			generator.writeNumberField("timestamp", result.getEpoch() / 1000);
			generator.writeObjectFieldStart("tags");

			for (Map.Entry<String, String> tag : this.tags.entrySet()) {
				generator.writeStringField(tag.getKey(), tag.getValue());
			}
			for (ResultAttribute resultAttribute : this.resultAttributesToWriteAsTags) {
				generator.writeStringField(resultAttribute.getName(), resultAttribute.get(result));
			}

			generator.writeEndObject();
			generator.writeEndObject();
			generator.close();
			return out.toString("UTF-8");
		}
	}
}
