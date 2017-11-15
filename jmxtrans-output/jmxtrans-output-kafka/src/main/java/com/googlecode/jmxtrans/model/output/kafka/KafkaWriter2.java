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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class KafkaWriter2 extends OutputWriterAdapter {
	@Nonnull
	@Getter(AccessLevel.PROTECTED)
	private final ObjectMapper objectMapper;
	@Nonnull
	@Getter
	private final Map<String, Object> producerConfig;
	@Nonnull
	@Getter
	private final String topic;
	private final Producer<String, String> producer;
	@Nonnull
	@Getter
	private final ResultSerializer resultSerializer;

	public KafkaWriter2(@Nonnull ObjectMapper objectMapper, @Nonnull Map<String, Object> producerConfig, @Nonnull String topic, @Nonnull ResultSerializer resultSerializer) {
		this.objectMapper = objectMapper;
		this.producerConfig = producerConfig;
		this.topic = topic;
		this.resultSerializer = resultSerializer;
		producer = new KafkaProducer<String, String>(producerConfig);
	}

	@VisibleForTesting
	KafkaWriter2(@Nonnull ObjectMapper objectMapper, Producer<String, String> producer, @Nonnull String topic, @Nonnull ResultSerializer resultSerializer) {
		this.objectMapper = objectMapper;
		this.producerConfig = Collections.emptyMap();
		this.topic = topic;
		this.resultSerializer = resultSerializer;
		this.producer = producer;
	}

	@Override
	public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
		for (Result result : results) {
			for (String message : resultSerializer.serialize(server, query, result)) {
				producer.send(new ProducerRecord<String, String>(topic, message));
			}
		}
	}

	@Override
	public void close() throws LifecycleException {
		producer.close();
	}
}
