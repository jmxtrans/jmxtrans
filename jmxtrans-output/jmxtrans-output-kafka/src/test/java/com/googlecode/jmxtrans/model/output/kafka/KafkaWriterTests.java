package com.googlecode.jmxtrans.model.output.kafka;

import java.util.HashMap;
import java.util.Map;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KafkaWriterTests {

	@Mock private Producer<String, String> producer;
	@Captor private ArgumentCaptor<KeyedMessage<String, String>> messageCaptor;

	@Test public void
	kafkaWriterNotNull() throws Exception {
		assertThat(getTestKafkaWriter()).isNotNull();
	}

	@Test public void
	messagesAreSentToKafka() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(1, "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		KafkaWriter writer = getTestKafkaWriter();
		writer.setProducer(producer);
		writer.doWrite(server, query, ImmutableList.of(result));

		verify(producer).send(messageCaptor.capture());
		KeyedMessage<String, String> message = messageCaptor.getValue();

		assertThat(message.topic()).isEqualTo("myTopic");
		assertThat(message.message())
				.contains("\"keyspace\":\"rootPrefix.host_123.classNameAlias.attributeName_key\"")
				.contains("\"value\":\"1\"")
				.contains("\"timestamp\":0");
	}
	
	private static KafkaWriter getTestKafkaWriter() {
		ImmutableList typenames = ImmutableList.of();
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("zk.connect", "host:2181");
		settings.put("metadata.broker.list", "10.231.1.1:9180");
		settings.put("serializer.class", "kafka.serializer.StringEncoder");
		settings.put("debug", false);
		settings.put("booleanAsNumber", true);
		settings.put("topics", "myTopic");
		return new KafkaWriter(typenames, true, "rootPrefix", true, "myTopic", settings);
	}
	
}
