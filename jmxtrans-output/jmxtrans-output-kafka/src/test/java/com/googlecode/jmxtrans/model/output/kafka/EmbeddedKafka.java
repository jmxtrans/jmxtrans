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

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.googlecode.jmxtrans.model.output.kafka.EmbeddedZookeeper.getResourceAsProperties;

public class EmbeddedKafka extends ExternalResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedKafka.class);
	private final TemporaryFolder temporaryFolder;
	private KafkaServerStartable server;
	private File logDir;

	public EmbeddedKafka(TemporaryFolder temporaryFolder) {
		this.temporaryFolder = temporaryFolder;
	}

	@Override
	public void before() throws IOException {
		LOGGER.info("Starting Kafka");
		logDir = temporaryFolder.newFolder("kafka");
		Properties properties = getResourceAsProperties("kafka.properties");
		properties.setProperty("log.dirs", logDir.getAbsolutePath());
		properties.setProperty("listeners", "PLAINTEXT://:9092");
		KafkaConfig config = new KafkaConfig(properties);
		server = new KafkaServerStartable(config);
		server.startup();
	}


	public Consumer<Long, String> createConsumer(String groupId) {
		Map<String, Object> consumerConfig = new HashMap<>();
		consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
		consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerConfig.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
		return new KafkaConsumer<>(consumerConfig);
	}

	public List<String> consume(String topic, String groupId, long timeout) {
		List<String> messages = new ArrayList<>();
		long end = System.currentTimeMillis() + timeout;
		try (Consumer<Long, String> consumer = createConsumer(groupId)) {
			consumer.subscribe(Collections.singletonList(topic));
			while (messages.isEmpty() && System.currentTimeMillis() < end) {
				ConsumerRecords<Long, String> records = consumer.poll(timeout / 10L);
				for (ConsumerRecord<Long, String> record : records) {
					messages.add(record.value());
				}
			}
		}
		return messages;
	}

	@Override
	public void after() {
		LOGGER.info("Stopping Kafka");
		server.shutdown();
	}
}
