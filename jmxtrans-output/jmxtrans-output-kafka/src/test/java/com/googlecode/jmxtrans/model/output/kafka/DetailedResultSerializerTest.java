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
import org.junit.Test;

import java.util.Collection;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.hashResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DetailedResultSerializerTest {
	@Test
	public void convertSingleToStringWhenMultiValues() throws Exception {
		ResultSerializer resultSerializer = new DetailedResultSerializer(false);

		Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResult());

		assertThat(messages).hasSize(1);

		String message = messages.iterator().next();
		// Check JSON Syntax and detailed content
		JsonNode jsonNode = new ObjectMapper().readValue(message, JsonNode.class);
		assertThat(jsonNode.get("host").asText()).isEqualTo("host.example.net");
		assertThat(jsonNode.get("source").asText()).isEqualTo("host.example.net");
		assertThat(jsonNode.get("port").asInt()).isEqualTo(4321);
		assertThat(jsonNode.get("attributeName").asText()).isEqualTo("ObjectPendingFinalizationCount");
		assertThat(jsonNode.get("className").asText()).isEqualTo("sun.management.MemoryImpl");
		assertThat(jsonNode.get("objDomain").asText()).isEqualTo("ObjectDomainName");
		assertThat(jsonNode.get("typeName").asText()).isEqualTo("type=Memory");
		assertThat(jsonNode.get("typeNameMap").get("type").asText()).isEqualTo("Memory");
		assertThat(jsonNode.get("typeNameMap")).hasSize(1);
		assertThat(jsonNode.get("values").get("ObjectPendingFinalizationCount").asLong()).isEqualTo(10L);
		assertThat(jsonNode.get("epoch").asLong()).isEqualTo(0L);
		assertThat(jsonNode.get("keyAlias").asText()).isEqualTo("MemoryAlias");
	}

	@Test
	public void convertHashToStringsWhenMultiValues() throws Exception {
		ResultSerializer resultSerializer = new DetailedResultSerializer(false);

		Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), hashResult());

		assertThat(messages).hasSize(1);
		for (String message : messages) {
			assertThat(message)
					.contains("\"host\":\"host.example.net\"")
					.contains("\"attributeName\":\"NonHeapMemoryUsage\"")
					.contains("\"typeName\":\"type=Memory\"")
					.contains("\"values\":")
					.contains("\"committed\":12345")
					.contains("\"epoch\":0")
					.contains("\"keyAlias\":\"MemoryAlias\"");

		}
	}

	@Test
	public void convertSingleToStringWhenSingleValue() throws Exception {
		ResultSerializer resultSerializer = new DetailedResultSerializer(true);

		Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResult());

		assertThat(messages).hasSize(1);
		String message = messages.iterator().next();
		assertThat(message)
				.contains("\"host\":\"host.example.net\"")
				.contains("\"attributeName\":\"ObjectPendingFinalizationCount\"")
				.contains("\"typeName\":\"type=Memory\"")
				.contains("\"valueName\":\"ObjectPendingFinalizationCount\"")
				.contains("\"value\":10")
				.contains("\"epoch\":0")
				.contains("\"keyAlias\":\"MemoryAlias\"");
	}

	@Test
	public void convertHashToStringsWhenSingleValue() throws Exception {
		ResultSerializer resultSerializer = new DetailedResultSerializer(true);

		Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), hashResult());

		assertThat(messages).hasSize(4);
		for (String message : messages) {
			assertThat(message)
					.contains("\"host\":\"host.example.net\"")
					.contains("\"attributeName\":\"NonHeapMemoryUsage\"")
					.contains("\"typeName\":\"type=Memory\"")
					.contains("\"value\":")
					.contains("\"epoch\":0")
					.contains("\"keyAlias\":\"MemoryAlias\"");

		}
	}

	@Test
	public void equalsHashCodeWhenSame() throws Exception {
		DetailedResultSerializer resultSerializer1 = new DetailedResultSerializer(true);
		DetailedResultSerializer resultSerializer2 = new DetailedResultSerializer(true);

		assertThat(resultSerializer1).isEqualTo(resultSerializer2);
		assertThat(resultSerializer1.hashCode()).isEqualTo(resultSerializer2.hashCode());
	}

	@Test
	public void equalsWhenDifferent() throws Exception {
		DetailedResultSerializer resultSerializer1 = new DetailedResultSerializer(true);
		DetailedResultSerializer resultSerializer2 = new DetailedResultSerializer(false);

		assertThat(resultSerializer1).isNotEqualTo(resultSerializer2);
	}

}
