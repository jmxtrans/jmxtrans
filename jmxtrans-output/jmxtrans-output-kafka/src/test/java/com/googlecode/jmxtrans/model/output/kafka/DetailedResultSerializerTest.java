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

import org.junit.Test;

import java.util.Collection;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.hashResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

public class DetailedResultSerializerTest {
    @Test
    public void convertSingleToStringWhenMultiValues() throws Exception {
        ResultSerializer resultSerializer = new DetailedResultSerializer(false);

        Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), numericResult());

        assertThat(messages).hasSize(1);
        String message = messages.iterator().next();
        assertThat(message)
                .contains("\"host\":\"host.example.net\"")
                .contains("\"attributeName\":\"ObjectPendingFinalizationCount\"")
                .contains("\"typeName\":\"type=Memory\"")
                .contains("\"ObjectPendingFinalizationCount\":10")
                .contains("\"epoch\":0")
                .contains("\"keyAlias\":\"MemoryAlias\"");
    }

    @Test
    public void convertHashToStringsWhenMultiValues() throws Exception {
        ResultSerializer resultSerializer = new DetailedResultSerializer(false);

        Collection<String> messages = resultSerializer.serialize(dummyServer(), dummyQuery(), hashResult());

        assertThat(messages).hasSize(1);
        for(String message: messages) {
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
        for(String message: messages) {
            assertThat(message)
                    .contains("\"host\":\"host.example.net\"")
                    .contains("\"attributeName\":\"NonHeapMemoryUsage\"")
                    .contains("\"typeName\":\"type=Memory\"")
                    .contains("\"value\":")
                    .contains("\"epoch\":0")
                    .contains("\"keyAlias\":\"MemoryAlias\"");

        }
    }

}