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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.googlecode.jmxtrans.model.ServerFixtures.DEFAULT_HOST;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KafkaWriter2Test {

    public static final String TOPIC = "topic";
    private final ObjectMapper objectMapper =  new ObjectMapper();
    @Mock
    public Producer<String, String> producerMock;

    @Test
    public void testWrite() throws Exception {
        // Given
        KafkaWriter2 writer = new KafkaWriter2(
                objectMapper,
                producerMock,
                TOPIC,
                new DetailedResultSerializer());
        ArgumentCaptor<ProducerRecord> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        when(producerMock.send(recordCaptor.capture())).thenReturn(null);
        // When
        writer.doWrite(dummyServer(), null, dummyResults());
        // Then
        verify(producerMock, times(3)).send(any(ProducerRecord.class));
        List<ProducerRecord> records = recordCaptor.getAllValues();
        assertThat(records).hasSize(3);
        for(ProducerRecord<String, String> record: records) {
            assertThat(record.topic()).isEqualTo("topic");
            JsonNode jsonNode = objectMapper.readValue(record.value(), JsonNode.class);
            assertThat(jsonNode.get("host").asText()).isEqualTo(DEFAULT_HOST);
            assertThat(jsonNode.get("epoch").asLong()).isEqualTo(0L);
            assertThat(jsonNode.get("value").asLong()).isNotNull();
        }
    }

	@Test
	public void producerClosed() throws Exception {
		// Given
		KafkaWriter2 writer = new KafkaWriter2(
				objectMapper,
				producerMock,
				TOPIC,
				new DetailedResultSerializer());

		writer.close();

		verify(producerMock).close();
	}

}
