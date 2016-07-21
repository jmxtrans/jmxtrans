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
package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import com.googlecode.jmxtrans.model.output.Settings;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
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

    private Producer<String, String> producer;
    private final Iterable<String> topics;
    private final String rootPrefix;
    private final ImmutableMap<String, String> tags;

    @JsonCreator
    public KafkaWriter(
            @JsonProperty("typeNames") ImmutableList<String> typeNames,
            @JsonProperty("booleanAsNumber") boolean booleanAsNumber,
            @JsonProperty("rootPrefix") String rootPrefix,
            @JsonProperty("debug") Boolean debugEnabled,
            @JsonProperty("topics") String topics,
            @JsonProperty("tags") Map<String, String> tags,
            @JsonProperty("settings") Map<String, Object> settings) {
        super(typeNames, booleanAsNumber, debugEnabled, settings);
        this.rootPrefix = firstNonNull(
                rootPrefix,
                (String) getSettings().get("rootPrefix"),
                DEFAULT_ROOT_PREFIX);
        // Setting all the required Kafka Properties
        Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("metadata.broker.list", Settings.getStringSetting(settings, "metadata.broker.list", null));
        kafkaProperties.setProperty("zk.connect", Settings.getStringSetting(settings, "zk.connect", null));
        kafkaProperties.setProperty("serializer.class", Settings.getStringSetting(settings, "serializer.class", null));
        this.producer = new Producer<>(new ProducerConfig(kafkaProperties));
        this.topics = asList(Settings.getStringSetting(settings, "topics", "").split(","));
        this.tags = ImmutableMap.copyOf(firstNonNull(tags, (Map<String, String>) getSettings().get("tags"), ImmutableMap.<String, String>of()));
        jsonFactory = new JsonFactory();
    }

    @Override
    public void validateSetup(Server server, Query query) throws ValidationException {
    }

    @Override
    protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {

		/*
        String message = createIdathaMessage(server, query, results);
		for(String topic : this.topics) {
			log.debug("Topic: [{}] ; Kafka Message: [{}]", topic, message);
			producer.send(new KeyedMessage<String, String>(topic, message));
		}
		*/
        log.debug("RESULT : "+results.toString());

        Map<CompounedKey, Set<Result>> metrics = new HashMap<>();

        for (Result result : results) {
            log.debug("Query result: [{}]", result);
            Map<String, Object> resultValues = result.getValues();
            for (Entry<String, Object> values : resultValues.entrySet()) {
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

        for (Entry<CompounedKey, Set<Result>> entry : metrics.entrySet()) {
            String message = createIdathaMessage(server.getHost(), entry.getKey().getTimestamp(), entry.getKey().getWhat(), entry.getKey().getDevice(), entry.getValue());
            for(String topic : this.topics) {
                log.debug("Topic: [{}] ; Kafka Message: [{}]", topic, message);
                producer.send(new KeyedMessage<String, String>(topic, message));
            }
        }
    }


    private String createIdathaMessage(String host, Long timestmap, String what, String device, Collection<Result> results) throws IOException {

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
                for (Entry<String, Object> values : resultValues.entrySet()) {
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
            generator.writeStringField("issuer", "jmxtrans");
            for (Entry<String, String> tag : this.tags.entrySet()) {
                generator.writeStringField(tag.getKey(), tag.getValue());
            }
            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
            return out.toString("UTF-8");
        }

    }

    private String createJsonMessage(Server server, Query query, List<String> typeNames, Result result, Entry<String, Object> values, Object value) throws IOException {
        //String keyString = getKeyString(server, query, result, values, typeNames, this.rootPrefix);
        //String cleanKeyString = keyString.replaceAll("[()]", "_");

        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                JsonGenerator generator = jsonFactory.createGenerator(out, UTF8)
        ) {
            generator.writeStartObject();
            generator.writeStringField("what", result.getAttributeName());
            generator.writeNumberField("timestamp", result.getEpoch() / 1000);
            generator.writeStringField("host", server.getHost());
            generator.writeStringField("device", result.getTypeName().replace("type=", "").replace(",name=", "_").replaceAll("\\(|\\)|\\[|\\]|=|,|\\s", "_"));

            if (isNumeric(value)) {
                generator.writeFieldName(values.getKey());
                String formatedValue = new DecimalFormat("###############.####").format(value);
                generator.writeNumber(formatedValue);
            } else {
                generator.writeStringField(values.getKey(), value.toString());
            }

            generator.writeObjectFieldStart("meta");
            generator.writeStringField("issuer", "jmxtrans");
            for (Entry<String, String> tag : this.tags.entrySet()) {
                generator.writeStringField(tag.getKey(), tag.getValue());
            }

            generator.writeEndObject();
            generator.writeEndObject();
            generator.close();
            return out.toString("UTF-8");
        }
    }


    @VisibleForTesting
    void setProducer(Producer<String, String> producer) {
        this.producer = producer;
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
