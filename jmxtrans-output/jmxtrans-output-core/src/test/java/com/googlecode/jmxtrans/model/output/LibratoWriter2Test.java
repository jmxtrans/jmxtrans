/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
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

import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class LibratoWriter2Test {

	@Test
	public void metricsAreFormattedCorrectly() throws IOException {
		LibratoWriter2 libratoWriter = new LibratoWriter2(new JsonFactory(), ImmutableList.<String>of());

		StringWriter writer = new StringWriter();

		libratoWriter.write(writer, dummyServer(), dummyQuery(), dummyResults());

		String json = writer.toString();

		assertThatJson(json)
				.node("counters").isArray().ofLength(0);
		assertThatJson(json)
				.node("gauges").isArray().ofLength(1);
		assertThatJson(json)
				.node("gauges[0].name").isEqualTo("ObjectPendingFinalizationCount.ObjectPendingFinalizationCount")
				.node("gauges[0].source").isEqualTo("host_example_net")
				.node("gauges[0].measure_time").isEqualTo(0)
				.node("gauges[0].value").isEqualTo(10);
	}

}
