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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Result;
import org.junit.Test;

import java.util.Collection;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.*;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultResultSerializerTest {
	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Create result with non null epoch
     */
	private static Result numericResultAt(long timestamp) {
		Result result = numericResult();
		return new Result(timestamp, result.getAttributeName(), result.getClassName(), result.getObjDomain(), result.getKeyAlias(), result.getTypeName(), result.getValuePath(), result.getValue());
	}
	@Test
	public void convertSingleNumericToString() throws Exception {
		ImmutableMap<String, String> tags = ImmutableMap.of("myTagKey1", "myTagValue1");
		ResultSerializer resultSerializer = new DefaultResultSerializer(ImmutableList.<String>of(), false, "rootPrefix", tags, asList("typeName.type", "className"));

		long now = System.currentTimeMillis();
		Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResultAt(now));

		assertThat(messages).hasSize(1);
		String message = messages.iterator().next();
		// Check JSON syntax
		assertThat(message).endsWith("}}");
		JsonNode jsonNode = objectMapper.readValue(message, JsonNode.class);
		assertThat(jsonNode.get("keyspace").asText()).isEqualTo("rootPrefix.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount");
		assertThat(jsonNode.get("value").asLong()).isEqualTo(10L);
		assertThat(jsonNode.get("timestamp").asLong()).isEqualTo(now / 1000);
		assertThat(jsonNode.get("tags").get("myTagKey1").asText()).isEqualTo("myTagValue1");
		assertThat(jsonNode.get("tags").get("typeName.type").asText()).isEqualTo("Memory");
		assertThat(jsonNode.get("tags").get("className").asText()).endsWith("MemoryImpl");
	}

	@Test
	public void convertSingleNonNumericToString() throws Exception {
		ImmutableMap<String, String> tags = ImmutableMap.of("myTagKey1", "myTagValue1");
		ResultSerializer resultSerializer = new DefaultResultSerializer(ImmutableList.<String>of(), false, "rootPrefix", tags, ImmutableList.<String>of());

		Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), stringResult());

		assertThat(messages).isEmpty();
	}

	@Test
	public void initDefaults() throws Exception {
		DefaultResultSerializer resultSerializer = new DefaultResultSerializer(null, false, null, null, null);

		assertThat(resultSerializer.getTypeNames()).isNotNull();
		assertThat(resultSerializer.getTypeNames()).isEmpty();
		assertThat(resultSerializer.getTags()).isNotNull();
		assertThat(resultSerializer.getTags()).isEmpty();
	}

	@Test
	public void equalsHashCodeWhenSame() throws Exception {
		DefaultResultSerializer resultSerializer1 = new DefaultResultSerializer(null, false, null, null, null);
		DefaultResultSerializer resultSerializer2 = new DefaultResultSerializer(null, false, null, null, null);

		assertThat(resultSerializer1).isEqualTo(resultSerializer2);
		assertThat(resultSerializer1.hashCode()).isEqualTo(resultSerializer2.hashCode());
	}

	@Test
	public void equalsWhenDifferent() throws Exception {
		DefaultResultSerializer resultSerializer1 = new DefaultResultSerializer(null, false, null, null, null);
		DefaultResultSerializer resultSerializer2 = new DefaultResultSerializer(asList("Type"), true, "root", null, null);

		assertThat(resultSerializer1).isNotEqualTo(resultSerializer2);
	}
}
