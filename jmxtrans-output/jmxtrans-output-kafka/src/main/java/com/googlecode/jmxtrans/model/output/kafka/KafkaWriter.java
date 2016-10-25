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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import com.googlecode.jmxtrans.model.output.Settings;
import kafka.javaapi.producer.Producer;
import org.apache.kafka.clients.producer.KafkaProducer;


import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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

    private static final String DEFAULT_ROOT_PREFIX = "servers";
    private static final String DEFAULT_FORMATTER = DefaultMessageFormatter.class.getName();
    public static final String TYPE_NAMES_KEY = "typeNames";
    public static final String ROOT_PREFIX_KEY = "rootPrefix";
    public static final String TAGS_KEY = "tags";
    public static final String TOPICS_KEY = "topics";
    private final KafkaMessageFormatter formatter;

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
            @JsonProperty("formatter") String formatter,
            @JsonProperty("settings") Map<String, Object> settings
    ) {
        super(typeNames, booleanAsNumber, debugEnabled, settings);
        this.rootPrefix = firstNonNull(
                rootPrefix,
                (String) getSettings().get("rootPrefix"),
                DEFAULT_ROOT_PREFIX);
        this.tags = ImmutableMap.copyOf(firstNonNull(tags, (Map<String, String>) getSettings().get("tags"), ImmutableMap.<String, String>of()));
        this.topics = asList(Settings.getStringSetting(settings, "topics", "").split(","));
        // Setting all the required Kafka Properties
        Properties kafkaProperties = new Properties();
        kafkaProperties.put("acks", "all");
        kafkaProperties.put("retries", 0);
        kafkaProperties.put("batch.size", 16384);
        kafkaProperties.put("linger.ms", 1);
        kafkaProperties.put("buffer.memory", 33554432);
        kafkaProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        for (Map.Entry<String, Object> stringObjectEntry : settings.entrySet()) {
            kafkaProperties.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }

        Map<String, Object> formatterProperties = new HashMap<>();
        formatterProperties.put(TYPE_NAMES_KEY, this.getTypeNames());
        formatterProperties.put(ROOT_PREFIX_KEY, this.rootPrefix);
        formatterProperties.put(TAGS_KEY, this.tags);
        formatterProperties.put(TOPICS_KEY, this.topics);

        KafkaProducer<String, String> producer;


        this.formatter = instanciateFormatter(formatter != null ? formatter : DEFAULT_FORMATTER, new KafkaProducer<String, String>(kafkaProperties), formatterProperties);
    }

    private KafkaMessageFormatter instanciateFormatter(String formatterClass, KafkaProducer<String, String> producer, Map<String, Object> formatterProperties) {
        try {
            Class<?> clazz = Class.forName(formatterClass);
            Constructor<?> constructor = clazz.getConstructor(KafkaProducer.class, Map.class);
            return (KafkaMessageFormatter) constructor.newInstance(producer, formatterProperties);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unkown formatter class : " + formatterClass);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new KafkaMessageFormatterException("Unable to instanciate formatter", e);
        }
    }

    @Override
    public void validateSetup(Server server, Query query) throws ValidationException {
    }

    @VisibleForTesting
    void setProducer(KafkaProducer<String, String> producer) {
        this.formatter.setProducer(producer);
    }

    @Override
    protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
        formatter.write(server, query, results);
    }

}
