package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.Nonnull;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@EqualsAndHashCode(exclude = {"objectMapper", "producerConfig"})
public class KafkaWriterFactory implements OutputWriterFactory<KafkaWriter2> {
    @Nonnull
    @Getter @JsonIgnore
    private final ObjectMapper objectMapper;
    @Nonnull
    @Getter
    private final Map<String, Object> producerConfig;
    @Nonnull
    @Getter
    private final String bootstrapServers;
    @Nonnull
    @Getter
    private final String topic;
    private static final String STRING_SERIALIZER_CLASS = StringSerializer.class.getName();

    @JsonCreator
    public KafkaWriterFactory(
            @JsonProperty("producerConfig") Map<String, Object> producerConfig,
            @JsonProperty("topic") String topic) {
        this.objectMapper = new ObjectMapper();
        checkNotNull(producerConfig);
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
        this.bootstrapServers = (String) producerConfig.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        checkNotNull(bootstrapServers);
        this.topic = topic;
        checkNotNull(topic);
    }

    @Nonnull
    @Override
    public KafkaWriter2 create() {
        return new KafkaWriter2(objectMapper, producerConfig, topic);
    }
}
