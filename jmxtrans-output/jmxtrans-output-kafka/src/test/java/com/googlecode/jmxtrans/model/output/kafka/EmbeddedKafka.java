package com.googlecode.jmxtrans.model.output.kafka;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.googlecode.jmxtrans.model.output.kafka.EmbeddedZookeeper.getResourceAsProperties;

public class EmbeddedKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedKafka.class);
    private KafkaServerStartable server;
    private final File logDir;

    public EmbeddedKafka(File logDir) {
        this.logDir = logDir;
    }

    public void before() throws IOException {
        LOGGER.info("Starting Kafka");
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

    public void after() {
        LOGGER.info("Stopping Kafka");
        server.shutdown();
    }
}
