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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import com.googlecode.jmxtrans.model.output.Settings;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

/**
 * This low latency and thread safe output writer sends data to a kafka topics in JSON format.
 * Kafka Topics can be passed as separated by commas such as kafka01,kafka02.
 *
 * @author : utkarsh bhatnagar
 * @github user : utkarshcmu
 * @email : utkarsh.cmu@gmail.com
 */

@NotThreadSafe
public class KafkaWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(KafkaWriter.class);

	private static final String DEFAULT_ROOT_PREFIX = "servers";

	@Getter(AccessLevel.PACKAGE)
	private final Producer<String, String> producer;
	private final Iterable<String> topics;
	private final ResultSerializer resultSerializer;

	@VisibleForTesting
	KafkaWriter(
			ImmutableList<String> typeNames,
			boolean booleanAsNumber,
			String rootPrefix,
			Boolean debugEnabled,
			String topics,
			Map<String, String> tags,
			Map<String, Object> settings,
			Producer<String, String> producer) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.producer = producer;
		this.topics = asList(Settings.getStringSetting(settings, "topics", "").split(","));
		String aRootPrefix = firstNonNull(
				rootPrefix,
				(String) getSettings().get("rootPrefix"),
				DEFAULT_ROOT_PREFIX);
		Map<String, String> aTags = firstNonNull(tags, (Map<String, String>) getSettings().get("tags"), ImmutableMap.<String, String>of());
		resultSerializer = new DefaultResultSerializer(typeNames, booleanAsNumber, aRootPrefix, aTags, ImmutableList.<String>of());
	}


	@JsonCreator
	public KafkaWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("topics") String topics,
			@JsonProperty("tags") Map<String, String> tags,
			@JsonProperty("settings") Map<String, Object> settings) {
		this(typeNames, booleanAsNumber, rootPrefix, debugEnabled, topics, tags, settings, createProducer(settings));
	}

	private static Producer<String, String> createProducer(Map<String, Object> settings) {
		// Setting all the required Kafka Properties
		Properties kafkaProperties = new Properties();
		kafkaProperties.setProperty(BOOTSTRAP_SERVERS_CONFIG, Settings.getStringSetting(settings, BOOTSTRAP_SERVERS_CONFIG, null));
		kafkaProperties.setProperty(KEY_SERIALIZER_CLASS_CONFIG, Settings.getStringSetting(settings, KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()));
		kafkaProperties.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, Settings.getStringSetting(settings, VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()));
		// Starts Kafka producer thread
		return new KafkaProducer<>(kafkaProperties);
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		for (Result result : results) {
			log.debug("Query result: [{}]", result);
			for (String message : resultSerializer.serialize(server, query, result)) {
				for (String topic : this.topics) {
					log.debug("Topic: [{}] ; Kafka Message: [{}]", topic, message);
					producer.send(new ProducerRecord<String, String>(topic, message));
				}
			}
		}
	}

	@Override
	public void close() {
		producer.close();
	}
}
