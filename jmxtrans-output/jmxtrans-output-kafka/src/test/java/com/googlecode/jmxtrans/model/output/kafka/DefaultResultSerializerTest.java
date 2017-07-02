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
import org.junit.Test;

import java.util.Collection;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.hashResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultResultSerializerTest {
    @Test
    public void convertSingleToString() throws Exception {
        ImmutableMap<String, String> tags = ImmutableMap.of("myTagKey1", "myTagValue1");
        ResultSerializer resultSerializer = new DefaultResultSerializer(ImmutableList.<String>of(), false, "rootPrefix", tags);

        Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResult());

        assertThat(messages).hasSize(1);
        String message = messages.iterator().next();
        assertThat(message)
                .contains("\"keyspace\":\"rootPrefix.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount\"")
                .contains("\"value\":\"10\"")
                .contains("\"timestamp\":0")
                .contains("\"tags\":{\"myTagKey1\":\"myTagValue1\"");
    }

    @Test
    public void convertHashToStrings() throws Exception {
        ImmutableMap<String, String> tags = ImmutableMap.of("myTagKey1", "myTagValue1");
        ResultSerializer resultSerializer = new DefaultResultSerializer(ImmutableList.<String>of(), false, "rootPrefix", tags);

        Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), hashResult());

        assertThat(messages).hasSize(4);
        for(String message: messages) {
            assertThat(message)
                    .contains("\"keyspace\":\"rootPrefix.host_example_net_4321.MemoryAlias.NonHeapMemoryUsage_")
                    .contains("\"value\":")
                    .contains("\"timestamp\":0")
                    .contains("\"tags\":{\"myTagKey1\":\"myTagValue1\"");

        }
    }
}