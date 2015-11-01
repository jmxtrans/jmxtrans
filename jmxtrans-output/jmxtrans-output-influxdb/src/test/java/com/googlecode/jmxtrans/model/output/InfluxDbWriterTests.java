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

import static com.google.common.collect.Sets.immutableEnumSet;
import static com.googlecode.jmxtrans.model.output.InfluxDbWriter.TAG_HOSTNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JsonUtils;

/**
 * Tests for {@link InfluxDbWriter}.
 *
 * @author <a href="https://github.com/sihutch">github.com/sihutch</a>
 */

@RunWith(MockitoJUnitRunner.class)
public class InfluxDbWriterTests {

	private static final String DATABASE_NAME = "database";
	private static final String HOST = "localhost";
	@Mock
	private InfluxDB influxDB;
	@Captor
	private ArgumentCaptor<BatchPoints> messageCaptor;
	
	private ConsistencyLevel DEFAULT_CONSISTENCY_LEVEL = ConsistencyLevel.ALL;
	private String DEFAULT_RETENTION_POLICY = "default";
	private ImmutableSet<ResultAttribute> DEFAULT_RESULT_ATTRIBUTES = immutableEnumSet(EnumSet.allOf(ResultAttribute.class));

	Server server = Server.builder().setHost("localhost").setPort("123").build();
	Query query = Query.builder().setObj("test").build();
	Result result = new Result(2l, "attributeName", "className", "objDomain", "keyAlias", "typeName",
			ImmutableMap.of("key", (Object) 1));
	ImmutableList<Result> results = ImmutableList.of(result);

	@Test
	public void pointsAreWrittenToInfluxDb() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();
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
		expectedTags.put(ResultAttribute.ATTRIBUTENAME.getAttributeName(), result.getAttributeName());
		expectedTags.put(ResultAttribute.CLASSNAME.getAttributeName(), result.getClassName());
		expectedTags.put(ResultAttribute.OBJDOMAIN.getAttributeName(), result.getObjDomain());
		expectedTags.put(ResultAttribute.TYPENAME.getAttributeName(), result.getTypeName());
		expectedTags.put(TAG_HOSTNAME, HOST);
		String lineProtocol = buildLineProtocol(result.getKeyAlias(), expectedTags);

		List<Point> points = batchPoints.getPoints();
		assertThat(points).hasSize(1);

		Point point = points.get(0);
		assertThat(point.lineProtocol()).startsWith(lineProtocol);
	}

	@Test
	public void writeConsistencyLevelsAreAppliedToBatchPointsBeingWritten() throws Exception {
		for (ConsistencyLevel consistencyLevel : ConsistencyLevel.values()) {
			InfluxDbWriter writer = getTestInfluxDbWriterWithWriteConsistency(consistencyLevel);

			writer.doWrite(server, query, results);

			verify(influxDB,atLeastOnce()).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			assertThat(batchPoints.getConsistency()).isEqualTo(consistencyLevel);
		}
	}

	@Test
	public void onlyRequestedResultPropertiesAreAppliedAsTags() throws Exception {
		for (ResultAttribute expectedResultTag : ResultAttribute.values()) {
			ImmutableSet<ResultAttribute> expectedResultTags = ImmutableSet.of(expectedResultTag);
			InfluxDbWriter writer = getTestInfluxDbWriterWithResultTags(expectedResultTags);
			writer.doWrite(server, query, results);

			verify(influxDB, atLeastOnce()).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			String lineProtocol = batchPoints.getPoints().get(0).lineProtocol();

			assertThat(lineProtocol).contains(expectedResultTag.getAttributeName());
			EnumSet<ResultAttribute> unexpectedResultTags = EnumSet.complementOf(EnumSet.of(expectedResultTag));
			for (ResultAttribute unexpectedResultTag : unexpectedResultTags) {
				assertThat(lineProtocol).doesNotContain(unexpectedResultTag.getAttributeName());
			}
		}
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

	private InfluxDbWriter getTestInfluxDbWriterWithDefaultSettings() {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL,DEFAULT_RETENTION_POLICY,DEFAULT_RESULT_ATTRIBUTES);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithResultTags(ImmutableSet<ResultAttribute> resultTags) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL,DEFAULT_RETENTION_POLICY, resultTags);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithWriteConsistency(ConsistencyLevel consistencyLevel) {
		return getTestInfluxDbWriter(consistencyLevel, DEFAULT_RETENTION_POLICY,DEFAULT_RESULT_ATTRIBUTES);
	}

	private InfluxDbWriter getTestInfluxDbWriter(ConsistencyLevel consistencyLevel, String retentionPolicy,
			ImmutableSet<ResultAttribute> resultTags) {
		return new InfluxDbWriter(influxDB,DATABASE_NAME, consistencyLevel, retentionPolicy, resultTags);
	}
}