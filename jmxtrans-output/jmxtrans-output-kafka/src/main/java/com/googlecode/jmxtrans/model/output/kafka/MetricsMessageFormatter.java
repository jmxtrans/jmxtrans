package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import kafka.javaapi.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.*;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * Created by seuf on 25/07/2016.
 */
public class MetricsMessageFormatter implements KafkaMessageFormatter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JsonFactory jsonFactory;

    private final Map<String, String> tags;
    private final Iterable<String> topics;
    private KafkaProducer<String, String> producer;

    public MetricsMessageFormatter(KafkaProducer<String, String> producer, Map<String, Object> properties) {
        this.producer = producer;
        this.tags = (Map<String, String>) properties.get(KafkaWriter.TAGS_KEY);
        this.topics = (Iterable<String>) properties.get(KafkaWriter.TOPICS_KEY);
        this.jsonFactory = new JsonFactory();
    }

    @Override
    public void write(Server server, Query query, ImmutableList<Result> results) throws Exception {
        Map<CompounedKey, Set<Result>> metrics = new HashMap<>();

        for (Result result : results) {
            log.debug("Query result: [{}]", result);
            Map<String, Object> resultValues = result.getValues();
            for (Map.Entry<String, Object> values : resultValues.entrySet()) {
                String what = result.getTypeName().replace("type=", "").replaceAll(",name=.*", "");
                String device = "";
                if (result.getTypeName().contains("name=")) {
                    device = result.getTypeName().replaceAll("type=.*,", "").replace("name=", "");
                }
                Long timestamp = result.getEpoch() / 1000;

                CompounedKey key = new CompounedKey(timestamp, what, device);
                if (metrics.containsKey(key)) {
                    metrics.get(key).add(result);
                } else {
                    metrics.put(key, Sets.newHashSet(result));
                }
            }
        }

        for (
                Map.Entry<CompounedKey, Set<Result>> entry : metrics.entrySet()) {
            String message = createIdathaMessage(server.getHost(), entry.getKey().getTimestamp(), entry.getKey().getWhat(), entry.getKey().getDevice(), entry.getValue());
            for (String topic : this.topics) {
                log.debug("Topic: [{}] ; Kafka Message: [{}]", topic, message);
                producer.send(new ProducerRecord<String, String>(topic, message));
            }
        }

    }

    @Override
    public void setProducer(KafkaProducer<String, String> producer) {
        this.producer = producer;
    }


    private String createIdathaMessage(String host, Long timestmap, String what, String device, Iterable<Result> results) throws IOException {

        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                JsonGenerator generator = jsonFactory.createGenerator(out, UTF8)
        ) {
            generator.writeStartObject();
            generator.writeStringField("host", host);
            generator.writeStringField("what", what);
            generator.writeStringField("device", device);
            generator.writeNumberField("timestamp", timestmap);

            for (Result result : results) {
                Map<String, Object> resultValues = result.getValues();
                for (Map.Entry<String, Object> values : resultValues.entrySet()) {
                    Object value = values.getValue();
                    String field_name;
                    if (result.getAttributeName().compareTo(values.getKey()) == 0) {
                        field_name = values.getKey();
                    } else {
                        field_name = result.getAttributeName() + "_" + values.getKey();
                    }
                    if (isNumeric(value)) {
                        generator.writeFieldName(field_name);
                        String formatedValue = new DecimalFormat("###############.####").format(value);
                        generator.writeNumber(formatedValue);
                    } else {
                        generator.writeStringField(field_name, value.toString());
                    }
                }
            }
            generator.writeObjectFieldStart("meta");
            generator.writeStringField("issuer", "jmxtrans_" + InetAddress.getLocalHost().getHostName());
            for (Map.Entry<String, String> tag : this.tags.entrySet()) {
                generator.writeStringField(tag.getKey(), tag.getValue());
            }
            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
            return out.toString("UTF-8");
        }

    }

    /**
     * The MIT License
     * Copyright © 2010 JmxTrans team
     * <p/>
     * Permission is hereby granted, free of charge, to any person obtaining a copy
     * of this software and associated documentation files (the "Software"), to deal
     * in the Software without restriction, including without limitation the rights
     * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     * copies of the Software, and to permit persons to whom the Software is
     * furnished to do so, subject to the following conditions:
     * <p/>
     * The above copyright notice and this permission notice shall be included in
     * all copies or substantial portions of the Software.
     * <p/>
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
     * THE SOFTWARE.
     */
    private static final class CompounedKey {
        private Long timestamp;
        private String what;
        private String device;

        public CompounedKey(Long timestamp, String what, String device) {
            this.timestamp = timestamp;
            this.what = what;
            this.device = device;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public String getWhat() {
            return what;
        }

        public String getDevice() {
            return device;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompounedKey that = (CompounedKey) o;
            return Objects.equals(timestamp, that.timestamp) &&
                    Objects.equals(what, that.what) &&
                    Objects.equals(device, that.device);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, what, device);
        }
    }

}
