package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.googlecode.jmxtrans.model.naming.KeyUtils.getKeyString;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static com.fasterxml.jackson.core.JsonEncoding.UTF8;

/**
 * Created by elludo, seuf on 25/07/2016.
 */
public class DefaultMessageFormatter implements KafkaMessageFormatter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Iterable<String> topics;
    private final Map<String, String> tags;
    private final List<String> typeNames;
    private final String rootPrefix;

    private KafkaProducer<String, String> producer;
    private JsonFactory jsonFactory;

    public DefaultMessageFormatter(KafkaProducer<String, String> producer, Map<String, Object> properties) {
        this.producer = producer;
        this.tags = (Map<String, String>) properties.get(KafkaWriter.TAGS_KEY);
        this.topics = (Iterable<String>) properties.get(KafkaWriter.TOPICS_KEY);
        this.rootPrefix = (String) properties.get(KafkaWriter.ROOT_PREFIX_KEY);
        this.typeNames = (List<String>) properties.get(KafkaWriter.TYPE_NAMES_KEY);
        this.jsonFactory = new JsonFactory();
    }

    @Override
    public void write(Server server, Query query, ImmutableList<Result> results) throws Exception {
        for (Result result : results) {
            log.debug("Query result: [{}]", result);
            Map<String, Object> resultValues = result.getValues();
            for (Map.Entry<String, Object> values : resultValues.entrySet()) {
                Object value = values.getValue();
                if (isNumeric(value)) {
                    String message = createJsonMessage(server, query, result, values, value);
                    for (String topic : this.topics) {
                        log.debug("Topic: [{}] ; Kafka Message: [{}]", topic, message);
                        producer.send(new ProducerRecord<String, String>(topic, message));
                    }
                } else {
                    log.warn("Unable to submit non-numeric value to Kafka: [{}] from result [{}]", value, result);
                }
            }
        }
    }

    @Override
    public void setProducer(KafkaProducer<String, String> producer) {
        this.producer = producer;
    }

    private String createJsonMessage(Server server, Query query, Result result, Map.Entry<String, Object> values, Object value) throws IOException {
        String keyString = getKeyString(server, query, result, values, typeNames, this.rootPrefix);
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

            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
            return out.toString("UTF-8");
        }
    }
}
