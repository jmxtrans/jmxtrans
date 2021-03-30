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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Key;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JsonFormat;
import com.googlecode.jmxtrans.model.ResultAttributes;
import org.influxdb.InfluxDB;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.googlecode.jmxtrans.guice.JmxTransModule.createInjector;
import static org.assertj.core.api.Assertions.assertThat;

public class InfluxDbWriterFactoryTests {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void configureParser() {
		objectMapper = createInjector(new JmxTransConfiguration()).getInstance(Key.get(ObjectMapper.class, JsonFormat.class));
	}

	private InputStream openResource(String resource) throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/" + resource);
		if (inputStream == null) {
			throw new FileNotFoundException("Resource " + resource + "not found");
		}
		return inputStream;
	}

	@Test
	public void testReadConfigDefault() throws IOException {
		try (InputStream inputStream = openResource("influxdb-default.json")) {
			InfluxDbWriterFactory writerFactory = (InfluxDbWriterFactory) objectMapper.readValue(inputStream, InfluxDbWriterFactory.class);
			assertThat(writerFactory.getDatabase()).isEqualTo("jmxtransDB");
			assertThat(writerFactory.getTags()).isEmpty();
			assertThat(writerFactory.getWriteConsistency()).isEqualTo(InfluxDB.ConsistencyLevel.ALL);
			assertThat(writerFactory.getRetentionPolicy()).isEqualTo(InfluxDbWriterFactory.DEFAULT_RETENTION_POLICY);
			assertThat(writerFactory.getResultAttributesToWriteAsTags()).isEqualTo(ImmutableSet.copyOf(ResultAttributes.values()));
			assertThat(writerFactory.getTypeNames()).isEmpty();
			assertThat(writerFactory.isBooleanAsNumber()).isFalse();
			assertThat(writerFactory.isAllowStringValues()).isFalse();
			assertThat(writerFactory.isCreateDatabase()).isTrue();
			assertThat(writerFactory.isTypeNamesAsTags()).isFalse();
			assertThat(writerFactory.isReportJmxPortAsTag()).isFalse();
			assertThat(writerFactory).isEqualTo(new InfluxDbWriterFactory(null, false, "http://localhost" , "someUser",
					"somePassword", "jmxtransDB", null, null, null,
					null, true, false, false, false));
		}
	}

	@Test
	public void testReadConfigDetailed() throws IOException {
		try (InputStream inputStream = openResource("influxdb-detailed.json")) {
			InfluxDbWriterFactory writerFactory = (InfluxDbWriterFactory) objectMapper.readValue(inputStream, InfluxDbWriterFactory.class);
			assertThat(writerFactory.getDatabase()).isEqualTo("jmxtransDB");
			assertThat(writerFactory.getTags()).hasSize(2)
					.containsEntry("custom", "tag1")
					.containsEntry("custom2", "tag2");
			assertThat(writerFactory.getWriteConsistency()).isEqualTo(InfluxDB.ConsistencyLevel.ONE);
			assertThat(writerFactory.getRetentionPolicy()).isEqualTo("customPolicy");
			assertThat(writerFactory.getResultAttributesToWriteAsTags()).containsExactly(ResultAttributes.TYPE_NAME, ResultAttributes.CLASS_NAME);
			assertThat(writerFactory.getTypeNames()).isEmpty();
			assertThat(writerFactory.isBooleanAsNumber()).isTrue();
			assertThat(writerFactory.isAllowStringValues()).isTrue();
			assertThat(writerFactory.isCreateDatabase()).isFalse();
			assertThat(writerFactory.isTypeNamesAsTags()).isTrue();
			assertThat(writerFactory.isReportJmxPortAsTag()).isTrue();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUrlEmpty() {
		new InfluxDbWriterFactory(null, false, null /* null url */, "username",
				"password", "database", null, null, null,
				null, false, false, false, false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUsernameEmpty() {
		new InfluxDbWriterFactory(null, false, "url" , null /* null username */,
				"password", "database", null, null, null,
				null, false, false, false, false);

	}
}
