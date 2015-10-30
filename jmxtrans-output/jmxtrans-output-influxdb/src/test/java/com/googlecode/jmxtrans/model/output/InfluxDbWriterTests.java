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

import static com.googlecode.jmxtrans.model.output.InfluxDbWriter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JsonUtils;

/**
 * Tests for {@link InfluxDbWriter}.
 *
 * @author Simon Hutchinson <https://github.com/sihutch>
 */

@RunWith(MockitoJUnitRunner.class)
public class InfluxDbWriterTests {

	private static final String DATABASE_NAME = "database";
	private static final String HOST = "localhost";
	@Mock
	private InfluxDB influxDB;
	@Captor
	private ArgumentCaptor<BatchPoints> messageCaptor;

	Server server = Server.builder().setHost("localhost").setPort("123").build();
	Query query = Query.builder().setObj("test").build();
	Result result = new Result(2l, "attributeName", "className", "objDomain", "keyAlias", "typeName",
			ImmutableMap.of("key", (Object) 1));
	ImmutableList<Result> results = ImmutableList.of(result);

	@Test
	public void pointsAreWrittenToInfluxDb() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();

		writer.setInfluxDB(influxDB);
		writer.doWrite(server, query, results);

		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();

		assertThat(batchPoints.getDatabase()).isEqualTo(DATABASE_NAME);

		// Point only exposes its state via a line protocol so we have to
		// make assertions against this.
		// Format is:
		// measurement,<comma separated key=val tags>" " <comma separated
		// key=val fields>
		Map<String, String> expectedTags = new TreeMap<String, String>();
		expectedTags.put(ResultAttribute.ATTRIBUTENAME.getTagName(), result.getAttributeName());
		expectedTags.put(ResultAttribute.CLASSNAME.getTagName(), result.getClassName());
		expectedTags.put(ResultAttribute.OBJDOMAIN.getTagName(), result.getObjDomain());
		expectedTags.put(ResultAttribute.TYPENAME.getTagName(), result.getTypeName());
		expectedTags.put(TAG_HOSTNAME, HOST);
		String lineProtocol = buildLineProtocol(result.getKeyAlias(), expectedTags);

		List<Point> points = batchPoints.getPoints();
		assertThat(points).hasSize(1);

		Point point = points.get(0);
		assertThat(point.lineProtocol()).startsWith(lineProtocol);
	}

	@Test
	public void allWriteConsistencyCanAppliedViaSettings() throws Exception {
		for (ConsistencyLevel consistencyLevel : ConsistencyLevel.values()) {
			InfluxDB mockInfluxDB = mock(InfluxDB.class);
			InfluxDbWriter writer = getTestInfluxDbWriterWithWriteConsistency(consistencyLevel);

			writer.setInfluxDB(mockInfluxDB);
			writer.doWrite(server, query, results);

			verify(mockInfluxDB).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			assertThat(batchPoints.getConsistency()).isEqualTo(consistencyLevel);
		}
	}

	@Test
	public void onlyRequestedResultPropertiesAreAppliedAsTags() throws Exception {
		for (ResultAttribute expectedResultTag : ResultAttribute.values()) {
			List<String> expectedResultTags = Arrays.asList(expectedResultTag.getTagName());
			InfluxDB mockInfluxDB = mock(InfluxDB.class);
			InfluxDbWriter writer = getTestInfluxDbWriterWithResultTags(expectedResultTags);

			writer.setInfluxDB(mockInfluxDB);
			writer.doWrite(server, query, results);

			verify(mockInfluxDB).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			String lineProtocol = batchPoints.getPoints().get(0).lineProtocol();

			assertThat(lineProtocol).contains(expectedResultTag.getTagName());
			EnumSet<ResultAttribute> unexpectedResultTags = EnumSet.complementOf(EnumSet.of(expectedResultTag));
			for (ResultAttribute unexpectedResultTag : unexpectedResultTags) {
				assertThat(lineProtocol).doesNotContain(unexpectedResultTag.getTagName());
			}
		}
	}

	@Test
	public void defaultWriteConsistencyIsAll() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();
		writer.setInfluxDB(influxDB);
		writer.doWrite(server, query, results);

		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();
		assertThat(batchPoints.getConsistency()).isEqualTo(ConsistencyLevel.ALL);
	}
	
	@Test
	public void defaultRetentionPolicyIsDefault() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();
		writer.setInfluxDB(influxDB);
		writer.doWrite(server, query, results);

		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();
		assertThat(batchPoints.getRetentionPolicy()).isEqualTo(DEFAULT_RETENTION_POLICY);
	}

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		File input = new File(InfluxDbWriterTests.class.getResource("/influxDB.json").toURI());
		JmxProcess process = JsonUtils.getJmxProcess(input);
		assertThat(process.getName()).isEqualTo("influxDB.json");
	}

	private String buildLineProtocol(String measurement, Map<String, String> expectedTags) {
		StringBuilder sb = new StringBuilder(measurement).append(",");
		int loops = 0;
		int tagCount = expectedTags.size();
		for (Map.Entry<String, String> entry : expectedTags.entrySet()) {
			loops++;
			sb.append(entry.getKey()).append("=").append(entry.getValue());
			if (loops < tagCount) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	private static InfluxDbWriter getTestInfluxDbWriterWithDefaultSettings() {
		return getTestInfluxDbWriter(null, null, null);
	}
	
	private static InfluxDbWriter getTestInfluxDbWriterWithResultTags(List<String> resultTags) {
		return getTestInfluxDbWriter(null, null, resultTags);
	}
	
	private static InfluxDbWriter getTestInfluxDbWriterWithWriteConsistency(ConsistencyLevel consistencyLevel) {
		return getTestInfluxDbWriter(consistencyLevel, null, null);
	}
	
	
	private static InfluxDbWriter getTestInfluxDbWriter(ConsistencyLevel consistencyLevel, String retentionPolicy, List<String> resultTags) {
		String writeConsistencyLevel = consistencyLevel == null ? null :  consistencyLevel.name();
		return new InfluxDbWriter(ImmutableList.<String> of(), false, false, "http://localhost:8086", "username",
				"password", DATABASE_NAME, writeConsistencyLevel, retentionPolicy, resultTags, null);
	}
}