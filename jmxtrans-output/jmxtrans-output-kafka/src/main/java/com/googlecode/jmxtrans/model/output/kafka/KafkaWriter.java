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
package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import com.googlecode.jmxtrans.model.output.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;
import static com.googlecode.jmxtrans.model.naming.KeyUtils.getKeyString;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.util.Arrays.asList;

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
	private final JsonFactory jsonFactory;

	private Producer<String,String> producer;
	private final Iterable<String> topics;
	private final String rootPrefix;

	@JsonCreator
	public KafkaWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("topics") String topics,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.rootPrefix = resolveProps(
				firstNonNull(
						rootPrefix,
						(String) getSettings().get("rootPrefix"),
						DEFAULT_ROOT_PREFIX));		
		// Setting all the required Kafka Properties
		Properties kafkaProperties =  new Properties();
		kafkaProperties.setProperty("metadata.broker.list", Settings.getStringSetting(settings, "metadata.broker.list", null));
		kafkaProperties.setProperty("zk.connect", Settings.getStringSetting(settings, "zk.connect", null));
		kafkaProperties.setProperty("serializer.class", Settings.getStringSetting(settings, "serializer.class", null));
		this.producer= new Producer<String,String>(new ProducerConfig(kafkaProperties));
		this.topics = asList(Settings.getStringSetting(settings, "topics", "").split(","));
		jsonFactory = new JsonFactory();
	}
	
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		List<String> typeNames = this.getTypeNames();

		for (Result result : results) {
			log.debug("Query result: [{}]", result);
			Map<String, Object> resultValues = result.getValues();
			for (Entry<String, Object> values : resultValues.entrySet()) {
				Object value = values.getValue();
				if (isNumeric(value)) {
					String message = createJsonMessage(server, query, typeNames, result, values, value);
					for(String topic : this.topics) {
						log.debug("Topic: [{}] ; Kafka Message: [{}]", topic, message);
						producer.send(new KeyedMessage<String, String>(topic, message));
					}
				} else {
					log.warn("Unable to submit non-numeric value to Kafka: [{}] from result [{}]", value, result);
				}
			}
		}
	}

	private String createJsonMessage(Server server, Query query, List<String> typeNames, Result result, Entry<String, Object> values, Object value) throws IOException {
		String keyString = getKeyString(server, query, result, values, typeNames, this.rootPrefix);
		String cleanKeyString = keyString.replaceAll("[()]", "_");

		Closer closer = Closer.create();
		try {
			ByteArrayOutputStream out = closer.register(new ByteArrayOutputStream());
			JsonGenerator generator = closer.register(jsonFactory.createGenerator(out, UTF8));
			generator.writeStartObject();
			generator.writeStringField("keyspace", cleanKeyString);
			generator.writeStringField("value", value.toString());
			generator.writeNumberField("timestamp", result.getEpoch() / 1000);
			generator.writeEndObject();
			generator.close();
			return out.toString("UTF-8");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@VisibleForTesting
	void setProducer(Producer<String, String> producer) {
		this.producer = producer;
	}

}
