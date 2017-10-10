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
import com.google.inject.Injector;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.ResultAttributes;
import com.googlecode.jmxtrans.util.ProcessConfigUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.googlecode.jmxtrans.model.output.InfluxDbWriter.TAG_HOSTNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link InfluxDbWriter}.
 *
 * @author <a href="https://github.com/sihutch">github.com/sihutch</a>
 */

@RunWith(MockitoJUnitRunner.class)
public class InfluxDbWriterTests {

	private static final String DATABASE_NAME = "database";
	private static final String HOST = "host.example.net";
	private static final ImmutableMap<String, String> DEFAULT_CUSTOM_TAGS = ImmutableMap.of();
	private static final ImmutableList<String> DEFAULT_TYPE_NAMES = ImmutableList.of();

	@Mock
	private InfluxDB influxDB;
	@Captor
	private ArgumentCaptor<BatchPoints> messageCaptor;

	private static final ConsistencyLevel DEFAULT_CONSISTENCY_LEVEL = ConsistencyLevel.ALL;
	private static final String DEFAULT_RETENTION_POLICY = "default";
	private static final ImmutableSet<ResultAttribute> DEFAULT_RESULT_ATTRIBUTES = ImmutableSet.copyOf(ResultAttributes.values());

	Result result = new Result(2l, "attributeName", "className", "objDomain", "keyAlias", "type=test,name=name",
			ImmutableList.of("key"), 1);
	ImmutableList<Result> results = ImmutableList.of(result);

	@Test
	public void pointsAreWrittenToInfluxDb() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();
		writer.doWrite(dummyServer(), dummyQuery(), results);

		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();

		assertThat(batchPoints.getDatabase()).isEqualTo(DATABASE_NAME);

		// Point only exposes its state via a line protocol so we have to
		// make assertions against this.
		// Format is:
		// measurement,<comma separated key=val tags>" " <comma separated
		// key=val fields>
		Map<String, String> expectedTags = new TreeMap<String, String>();
		expectedTags.put(ResultAttributes.ATTRIBUTE_NAME.getName() , result.getAttributeName());
		expectedTags.put(ResultAttributes.CLASS_NAME.getName(), result.getClassName());
		expectedTags.put(ResultAttributes.OBJ_DOMAIN.getName
					(), result.getObjDomain());
		expectedTags.put(TAG_HOSTNAME, HOST);
		String lineProtocol = buildLineProtocol(result.getKeyAlias(), expectedTags);

		List<Point> points = batchPoints.getPoints();
		assertThat(points).hasSize(1);

		Point point = points.get(0);
		assertThat(point.lineProtocol()).startsWith(lineProtocol);
	}

	@Test
	public void emptyCustomTagsDoesntBotherWrite() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();
		writer.doWrite(dummyServer(), dummyQuery(), results);
		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();

		assertThat(batchPoints.getDatabase()).isEqualTo(DATABASE_NAME);
		List<Point> points = batchPoints.getPoints();
		assertThat(points).hasSize(1);
	}

	@Test
	public void customTagsAreWrittenToDb() throws Exception {
		ImmutableMap<String, String> tags = ImmutableMap.of("customTag", "customValue");
		InfluxDbWriter writer = getTestInfluxDbWriterWithCustomTags(tags);
		writer.doWrite(dummyServer(), dummyQuery(), results);

		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();

		assertThat(batchPoints.getDatabase()).isEqualTo(DATABASE_NAME);
		List<Point> points = batchPoints.getPoints();
		assertThat(points).hasSize(1);

		Point point = points.get(0);
		assertThat(point.lineProtocol()).contains("customTag=customValue");
	}

	@Test
	public void attributeNameIncludesTypeNames() throws Exception {
		ImmutableList<String> typeNames = ImmutableList.of("name");
		InfluxDbWriter writer = getTestInfluxDbWriterWithTypeNames(typeNames);
		writer.doWrite(dummyServer(), dummyQuery(), results);

		verify(influxDB).write(messageCaptor.capture());
		BatchPoints batchPoints = messageCaptor.getValue();

		assertThat(batchPoints.getDatabase()).isEqualTo(DATABASE_NAME);
		List<Point> points = batchPoints.getPoints();
		assertThat(points).hasSize(1);

		Point point = points.get(0);
		assertThat(point.lineProtocol()).contains("name.attributeName_key");
	}

	@Test
	public void writeConsistencyLevelsAreAppliedToBatchPointsBeingWritten() throws Exception {
		for (ConsistencyLevel consistencyLevel : ConsistencyLevel.values()) {
			InfluxDbWriter writer = getTestInfluxDbWriterWithWriteConsistency(consistencyLevel);

			writer.doWrite(dummyServer(), dummyQuery(), results);

			verify(influxDB, atLeastOnce()).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			assertThat(batchPoints.getConsistency()).isEqualTo(consistencyLevel);
		}
	}

	@Test
	public void onlyRequestedResultPropertiesAreAppliedAsTags() throws Exception {
		for (ResultAttribute expectedResultTag : ResultAttributes.values()) {
			ImmutableSet<ResultAttribute> expectedResultTags = ImmutableSet.of(expectedResultTag);
			InfluxDbWriter writer = getTestInfluxDbWriterWithResultTags(expectedResultTags);
			writer.doWrite(dummyServer(), dummyQuery(), results);

			verify(influxDB, atLeastOnce()).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			String lineProtocol = batchPoints.getPoints().get(0).lineProtocol();

			assertThat(lineProtocol).contains(expectedResultTag.getName()+"=");
			Set<ResultAttribute> unexpectedResultTags = new HashSet<ResultAttribute>(ResultAttributes.values());
			unexpectedResultTags.remove(expectedResultTag);
			for (ResultAttribute unexpectedResultTag : unexpectedResultTags) {
				assertThat(lineProtocol).doesNotContain(unexpectedResultTag.getName()+"=");
			}
		}
	}

	@Test
	public void databaseIsCreated() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterWithDefaultSettings();
		writer.doWrite(dummyServer(), dummyQuery(), results);

		verify(influxDB).createDatabase(DATABASE_NAME);
	}

	@Test
	public void databaseIsNotCreated() throws Exception {
		InfluxDbWriter writer = getTestInfluxDbWriterNoDatabaseCreation();
		writer.doWrite(dummyServer(), dummyQuery(), results);

		verify(influxDB, never()).createDatabase(anyString());
	}

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		File input = new File(InfluxDbWriterTests.class.getResource("/influxDB.json").toURI());
		Injector injector = JmxTransModule.createInjector(new JmxTransConfiguration());
		ProcessConfigUtils processConfigUtils = injector.getInstance(ProcessConfigUtils.class);
		JmxProcess process = processConfigUtils.parseProcess(input);
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
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES,true);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithResultTags(ImmutableSet<ResultAttribute> resultTags) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, resultTags, DEFAULT_TYPE_NAMES,true);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithCustomTags(ImmutableMap<String, String> tags) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, tags, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES,true);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithTypeNames(ImmutableList<String> typeNames) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, typeNames,true);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithWriteConsistency(ConsistencyLevel consistencyLevel) {
		return getTestInfluxDbWriter(consistencyLevel, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES,true);
	}

	private InfluxDbWriter getTestInfluxDbWriterNoDatabaseCreation() {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES,false);
	}

	private InfluxDbWriter getTestInfluxDbWriter(ConsistencyLevel consistencyLevel, String retentionPolicy, ImmutableMap<String, String> tags,
												 ImmutableSet<ResultAttribute> resultTags, ImmutableList<String> typeNames, boolean createDatabase) {
		return new InfluxDbWriter(influxDB, DATABASE_NAME, consistencyLevel, retentionPolicy, tags, resultTags, typeNames, createDatabase);
	}
}
