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
package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.ResultAttribute;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.*;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleNumericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Nate Good on 10/21/16.
 */
public class StatsDTelegrafWriterTest {

    private StringWriter writer = new StringWriter();
    String expectedMeasureAndTags = "MemoryAlias,jmxport=4321,attribute=ObjectPendingFinalizationCount:"; //objectName=myQuery:key=val,

    @Test
    public void hashResultAttribues() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(new String[]{StatsDMetricType.COUNTER.getKey()}, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), hashResults());
        assertThat(writer.toString()).isEqualTo(
			"MemoryAlias,jmxport=4321,attribute=NonHeapMemoryUsage,resultKey=committed:12345|c\n" +
			"MemoryAlias,jmxport=4321,attribute=NonHeapMemoryUsage,resultKey=init:23456|c\n" +
			"MemoryAlias,jmxport=4321,attribute=NonHeapMemoryUsage,resultKey=used:45678|c\n");
    }

    @Test
    public void countAttribute() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(new String[]{StatsDMetricType.COUNTER.getKey()}, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), singleNumericResult());
        assertThat(writer.toString()).isEqualTo(expectedMeasureAndTags + "10|c\n");
    }

    @Test
    public void countAttribute_negativeValueIsIgnored() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(new String[]{StatsDMetricType.COUNTER.getKey()}, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of(numericResult(-10)));
        assertThat(writer.toString()).isEqualTo("");
    }

    @Test
    public void gaugeAttribute() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(new String[]{StatsDMetricType.GAUGE.getKey()}, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of(numericResult(-10)));
        assertThat(writer.toString()).isEqualTo(expectedMeasureAndTags + "-10|g\n");
    }

    @Test
    public void multipleBucketTypes() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(new String[]{"s","ms","g"}, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of( numericResult(5), numericResult(250), numericResult(10.5)));
        assertThat(writer.toString()).isEqualTo(
            expectedMeasureAndTags + "5|s\n" +
            expectedMeasureAndTags + "250|ms\n" +
            expectedMeasureAndTags + "10.5|g\n");
    }

	@Test
	public void nonNumericValue() throws IOException {
		StatsDTelegrafWriter w = new StatsDTelegrafWriter(new String[]{StatsDMetricType.GAUGE.getKey()}, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
		w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of(stringResult()));
		assertThat(writer.toString()).isEqualTo("");
	}
}
