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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rimal
 */
public class CloudWatchWriterTest {

	private CloudWatchWriter writer;

	@Test
	public void testDelegationToFactory() throws Exception {
		List<Map<String, Object>> dimensions = new ArrayList<>();
		dimensions.add(
				new HashMap<String, Object>() {{
					put("name", "InstanceId");
					put("value", "$InstanceId");
				}}
		);
		dimensions.add(
				new HashMap<String, Object>() {{
					put("name", "SomeKey");
					put("value", "SomeValue");
				}});

		writer = new CloudWatchWriter(ImmutableList.of("type"), false, false, "testNS", dimensions,
				new HashMap<String, Object>());

		CloudWatchWriterFactory writerFactory = writer.getCloudWatchWriterFactory();
		assertThat(writerFactory.getNamespace()).isEqualTo("testNS");
		assertThat(writerFactory.getTypeNames().size()).isEqualTo(1);
		assertThat(writerFactory.getTypeNames().get(0)).isEqualTo("type");
		assertThat(writerFactory.isBooleanAsNumber()).isFalse();

		ImmutableList<Map<String, Object>> dimensionsOfFactory = FluentIterable.from(
				writerFactory.getDimensions()).toList();
		assertThat(dimensionsOfFactory.get(0).get("name")).isEqualTo("InstanceId");
		assertThat(dimensionsOfFactory.get(0).get("value")).isEqualTo("$InstanceId");

		assertThat(dimensionsOfFactory.get(1).get("name")).isEqualTo("SomeKey");
		assertThat(dimensionsOfFactory.get(1).get("value")).isEqualTo("SomeValue");
	}
}
