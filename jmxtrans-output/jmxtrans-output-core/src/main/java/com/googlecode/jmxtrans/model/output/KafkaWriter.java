package com.googlecode.jmxtrans.model.output;

import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * This low latency and thread safe output writer sends data to a kafka topics in JSON format.
 * Kafka Topics can be passed as separated by commas such as kafka01,kafka02.
 *
 * @author utkarsh bhatnagar
 */

@NotThreadSafe
public class KafkaWriter extends BaseOutputWriter {
	
	private static final Logger log = LoggerFactory.getLogger(KafkaWriter.class);
	
	private static final String DEFAULT_ROOT_PREFIX = "servers";
	
	private Producer<String,String> producer;
	private String[] topics;
	private boolean isTest;
	private final String rootPrefix;

	@JsonCreator
	public KafkaWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("debug") Boolean debugEnabled,
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
		this.topics = Settings.getStringSetting(settings, "topics", null).split(","); 
	}
	
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {

		try {
			ArrayList<KeyedMessage<String,String>> messages = new ArrayList<KeyedMessage<String,String>>();
			List<String> typeNames = this.getTypeNames();

			for (Result result : results) {
				log.debug("Query result: {}", result);
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						Object value = values.getValue();
						if (NumberUtils.isNumeric(value)) {
							JSONObject obj = new JSONObject();
							obj.put("keyspace",KeyUtils.getKeyString(server, query, result, values, typeNames, this.rootPrefix).replaceAll("[()]", "_"));
							obj.put("value", value.toString());
							obj.put("timestamp", result.getEpoch() / 1000);
							for(String topic : this.topics) {
								log.debug("Topic: "+topic+" ; Kafka Message: {}", obj.toString());
								messages.add(new KeyedMessage<String,String>(topic,obj.toString()));
							}
						} else {
							log.warn("Unable to submit non-numeric value to Kafka: [{}] from result [{}]", value, result);
						}
					}
				}
			}
			if (!this.isTest)
				this.producer.send(messages);
			else
				log.debug("Topic: random Kafka Messages: {no messages for localhost}");
		} catch (Exception e){
			e.printStackTrace();
			log.error("Unable to write to kafka. Please debug.");
		}
	}
	
	public Producer<String, String> getProducer() {
		return producer;
	}

	public void setProducer(Producer<String, String> producer) {
		this.producer = producer;
	}

	public static Logger getLog() {
		return log;
	}
	
	public boolean isTest() {
		return isTest;
	}

	public void setTest(boolean isTest) {
		this.isTest = isTest;
	}

	
}
