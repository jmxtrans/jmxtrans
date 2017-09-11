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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@EqualsAndHashCode(exclude = {"objectMapper"})
public class KafkaWriterFactory implements OutputWriterFactory<KafkaWriter2> {
	@Nonnull
	@Getter(AccessLevel.PROTECTED)
	@JsonIgnore
	private final ObjectMapper objectMapper;
	@Nonnull
	@Getter
	private final Map<String, Object> producerConfig;
	@Nonnull
	@Getter
	private final String topic;
	@Nonnull
	@Getter
	private final ResultSerializer resultSerializer;

	private static final String STRING_SERIALIZER_CLASS = StringSerializer.class.getName();

	@JsonCreator
	public KafkaWriterFactory(
			@JsonProperty("producerConfig") @Nonnull Map<String, Object> producerConfig,
			@JsonProperty("topic") @Nonnull String topic,
			@JsonProperty("resultSerializer") ResultSerializer resultSerializer) {
		this.objectMapper = new ObjectMapper();
		ImmutableMap.Builder<String, Object> producerConfigBuilder = ImmutableMap.<String, Object>builder()
				.putAll(producerConfig);
		// Add default settings
		if (!producerConfig.containsKey(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)) {
			producerConfigBuilder.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, STRING_SERIALIZER_CLASS);
		}
		if (!producerConfig.containsKey(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)) {
			producerConfigBuilder.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, STRING_SERIALIZER_CLASS);
		}
		this.producerConfig = producerConfigBuilder.build();
		this.topic = topic;
		checkNotNull(topic);
		this.resultSerializer = resultSerializer == null ? new DefaultResultSerializer(
				Collections.<String>emptyList(),
				false, "",
				Collections.<String, String>emptyMap(),
				Collections.<String>emptyList()) : resultSerializer;
	}

	@Nonnull
	@Override
	public KafkaWriter2 create() {
		return new KafkaWriter2(objectMapper, producerConfig, topic, resultSerializer);
	}
}
