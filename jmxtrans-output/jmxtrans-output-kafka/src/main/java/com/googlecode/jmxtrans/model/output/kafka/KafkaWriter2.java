package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import lombok.Getter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

public class KafkaWriter2 extends OutputWriterAdapter {
    @Nonnull
    @Getter
    private final ObjectMapper objectMapper;
    @Nonnull
    @Getter
    private final Map<String, Object> producerConfig;
    @Nonnull
    @Getter
    private final String topic;
    private Producer<String, String> producer;

    public KafkaWriter2(@Nonnull ObjectMapper objectMapper, @Nonnull Map<String, Object> producerConfig, @Nonnull String topic) {
        this.objectMapper = objectMapper;
        this.producerConfig = producerConfig;
        this.topic = topic;
    }

    @Override
    public void start() throws LifecycleException {
        producer = new KafkaProducer<String, String>(producerConfig);
        // Force connection to broker and fetch partition assignments
        producer.partitionsFor(topic);
    }

    @Override
    public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
        for (Result result : results) {
            producer.send(createRecord(server, query, result));
        }
    }

    protected ProducerRecord<String, String> createRecord(Server server, Query query, Result result) throws Exception {
        return new ProducerRecord<String, String>(topic, createRecordValue(server, query, result));
    }

    protected String createRecordValue(Server server, Query query, Result result) throws Exception {
        return objectMapper.writeValueAsString(new KResult(server, result));
    }

    /**
     * DTO containing server and result information
     */
    @JsonSerialize(include = NON_NULL)
    @Immutable
    @ThreadSafe
    private static class KResult {
        // Server
        @Getter
        private final String alias;
        @Getter
        private final String pid;
        @Getter
        private final String host;
        @Getter
        private final String port;
        // Result
        @Getter
        private final String attributeName;
        @Getter
        private final String className;
        @Getter
        private final String objDomain;
        @Getter
        private final String typeName;
        @Getter
        private final Object value;
        @Getter
        private final ImmutableMap<String, Object> values;
        @Getter
        private final long epoch;
        @Getter
        private final String keyAlias;

        public KResult(Server server, Result result) {
            alias = server.getAlias();
            pid = server.getPid();
            host = server.getHost();
            port = server.getPort();
            attributeName = result.getAttributeName();
            className = result.getClassName();
            objDomain = result.getObjDomain();
            typeName = result.getTypeName();
            value = result.getValues().get(result.getAttributeName());
            if (result.getValues().size() > 1 || value != null) {
                values = result.getValues();
            } else {
                values = null;
            }
            epoch = result.getEpoch();
            keyAlias = result.getKeyAlias();
        }
    }

    @Override
    public void close() throws LifecycleException {
        producer.close();
    }
}
