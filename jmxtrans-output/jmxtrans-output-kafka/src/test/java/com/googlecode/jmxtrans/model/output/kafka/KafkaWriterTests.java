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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleNumericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KafkaWriterTests {

	@Mock
	private Producer<String, String> producer;
	@Captor
	private ArgumentCaptor<ProducerRecord<String, String>> messageCaptor;
	private Map<String, Object> settings;
	private final ImmutableMap<String, String> tags =	ImmutableMap.of("myTagKey1", "myTagValue1");

	@Before
	public void before() {
		settings = new HashMap<String, Object>();
		settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		settings.put("debug", false);
		settings.put("booleanAsNumber", true);
		settings.put("topics", "myTopic");
	}

	@Test
	public void messagesAreSentToKafka() throws Exception {
		try(KafkaWriter writer = new KafkaWriter(ImmutableList.<String>of(), true, "rootPrefix", true, "myTopic", tags, settings, producer)) {
			writer.doWrite(dummyServer(), dummyQuery(), singleNumericResult());

			verify(producer).send(messageCaptor.capture());
			ProducerRecord<String, String> message = messageCaptor.getValue();

			assertThat(message.topic()).isEqualTo("myTopic");
			assertThat(message.value())
					.contains("\"keyspace\":\"rootPrefix.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount\"")
					.contains("\"value\":\"10\"")
					.contains("\"timestamp\":0")
					.contains("\"tags\":{\"myTagKey1\":\"myTagValue1\"");
		}
	}

	@Test
	public void producerClosed() throws Exception {
		KafkaWriter writer = new KafkaWriter(ImmutableList.<String>of(), true, "rootPrefix", true, "myTopic", tags, settings, producer);

		writer.close();

		verify(producer).close();
	}

	@Test
	public void createKafkaProducer() throws Exception {
		try(KafkaWriter writer = new KafkaWriter(ImmutableList.<String>of(), true, "rootPrefix", true, "myTopic", tags, settings)) {
			assertThat(writer.getProducer()).isInstanceOf(KafkaProducer.class);
		}
	}

}
