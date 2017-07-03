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
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaWriterFactoryTest {
    private static ObjectMapper objectMapper = new ObjectMapper();

    private InputStream openResource(String resource) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/" + resource);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource " + resource + "not found");
        }
        return inputStream;
    }

    @Test
    public void testReadConfigDefault() throws IOException, LifecycleException {
        try (InputStream inputStream = openResource("kafka-writer-default.json")) {
            KafkaWriterFactory writerFactory = (KafkaWriterFactory) objectMapper.readValue(inputStream, KafkaWriterFactory.class);
            assertThat(writerFactory.getTopic()).isEqualTo("jmxtrans");
            assertThat(writerFactory.getProducerConfig().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
            assertThat(writerFactory.getResultSerializer()).isInstanceOf(DefaultResultSerializer.class);
            try (KafkaWriter2 writer = writerFactory.create()) {
                assertThat(writer.getTopic()).isEqualTo("jmxtrans");
                assertThat(writer.getProducerConfig().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
                assertThat(writer.getProducerConfig().get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG).toString()).endsWith("StringSerializer");
                assertThat(writer.getProducerConfig().get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG).toString()).endsWith("StringSerializer");
                assertThat(writer.getResultSerializer()).isInstanceOf(DefaultResultSerializer.class);
            }
        }
    }

    @Test
    public void testReadConfigDetailed() throws IOException, LifecycleException {
        try (InputStream inputStream = openResource("kafka-writer-detailed.json")) {
            KafkaWriterFactory writerFactory = (KafkaWriterFactory) objectMapper.readValue(inputStream, KafkaWriterFactory.class);
            assertThat(writerFactory.getTopic()).isEqualTo("jmxtrans");
            assertThat(writerFactory.getProducerConfig().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
            assertThat(writerFactory.getResultSerializer()).isInstanceOf(DetailedResultSerializer.class);
            assertThat(((DetailedResultSerializer) writerFactory.getResultSerializer()).isSingleValue()).isFalse();
            try (KafkaWriter2 writer = writerFactory.create()) {
                assertThat(writer.getTopic()).isEqualTo("jmxtrans");
                assertThat(writer.getResultSerializer()).isInstanceOf(DetailedResultSerializer.class);
            }
        }
    }

    @Test
    public void testReadConfigDetailedCustomized() throws IOException, LifecycleException {
        try (InputStream inputStream = openResource("kafka-writer-detailed2.json")) {
            KafkaWriterFactory writerFactory = (KafkaWriterFactory) objectMapper.readValue(inputStream, KafkaWriterFactory.class);
            assertThat(writerFactory.getTopic()).isEqualTo("jmxtrans");
            assertThat(writerFactory.getProducerConfig().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
            assertThat(writerFactory.getProducerConfig().get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo("KeySerializer");
            assertThat(writerFactory.getProducerConfig().get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo("ValueSerializer");
            assertThat(writerFactory.getResultSerializer()).isInstanceOf(DetailedResultSerializer.class);
        }
    }

    @Test
    public void testReadConfigDefaultCustomized() throws IOException, LifecycleException {
        try (InputStream inputStream = openResource("kafka-writer-default2.json")) {
            KafkaWriterFactory writerFactory = (KafkaWriterFactory) objectMapper.readValue(inputStream, KafkaWriterFactory.class);
            assertThat(writerFactory.getTopic()).isEqualTo("jmxtrans");
            assertThat(writerFactory.getProducerConfig().get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
            assertThat(writerFactory.getResultSerializer()).isInstanceOf(DefaultResultSerializer.class);
            DefaultResultSerializer resultSerializer = (DefaultResultSerializer) writerFactory.getResultSerializer();
            assertThat(resultSerializer.getRootPrefix()).isEqualTo("root");
            assertThat(resultSerializer.getTags().get("environment")).isEqualTo("dev");
            assertThat(resultSerializer.getTypeNames()).contains("Memory");
            assertThat(resultSerializer.isBooleanAsNumber()).isTrue();
            try (KafkaWriter2 writer = writerFactory.create()) {
                assertThat(writer.getTopic()).isEqualTo("jmxtrans");
                assertThat(writer.getResultSerializer()).isInstanceOf(DefaultResultSerializer.class);
            }
        }
    }

    @Test
    public void testEqualsHashCodeSame() throws IOException, LifecycleException {
        try (InputStream inputStream1 = openResource("kafka-writer-detailed2.json");
             InputStream inputStream2 = openResource("kafka-writer-detailed2.json")) {
            KafkaWriterFactory writerFactory1 = (KafkaWriterFactory) objectMapper.readValue(inputStream1, KafkaWriterFactory.class);
            KafkaWriterFactory writerFactory2 = (KafkaWriterFactory) objectMapper.readValue(inputStream2, KafkaWriterFactory.class);
            assertThat(writerFactory1.hashCode()).isEqualTo(writerFactory2.hashCode());
            assertThat(writerFactory1).isEqualTo(writerFactory2);
        }
    }

    @Test
    public void testEqualsHashCodeDifferent() throws IOException, LifecycleException {
        try (InputStream inputStream1 = openResource("kafka-writer-detailed2.json");
             InputStream inputStream2 = openResource("kafka-writer-detailed.json")) {
            KafkaWriterFactory writerFactory1 = (KafkaWriterFactory) objectMapper.readValue(inputStream1, KafkaWriterFactory.class);
            KafkaWriterFactory writerFactory2 = (KafkaWriterFactory) objectMapper.readValue(inputStream2, KafkaWriterFactory.class);
            assertThat(writerFactory1.hashCode()).isNotEqualTo(writerFactory2.hashCode());
        }
    }
}
