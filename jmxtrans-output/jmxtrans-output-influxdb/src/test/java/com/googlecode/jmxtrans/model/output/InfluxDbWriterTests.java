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

import com.googlecode.jmxtrans.util.JsonUtils;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.collect.Sets.immutableEnumSet;
import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.googlecode.jmxtrans.model.output.InfluxDbWriter.TAG_HOSTNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
	private static final ImmutableSet<ResultAttribute> DEFAULT_RESULT_ATTRIBUTES = immutableEnumSet(EnumSet.allOf(ResultAttribute.class));

	Result result = new Result(2l, "attributeName", "className", "objDomain", "keyAlias", "type=test,name=name",
			ImmutableMap.of("key", (Object) 1));
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
		expectedTags.put(enumValueToAttribute(ResultAttribute.ATTRIBUTE_NAME), result.getAttributeName());
		expectedTags.put(enumValueToAttribute(ResultAttribute.CLASS_NAME), result.getClassName());
		expectedTags.put(enumValueToAttribute(ResultAttribute.OBJ_DOMAIN), result.getObjDomain());
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
		assertThat(point.lineProtocol()).contains("name.key");
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
		for (ResultAttribute expectedResultTag : ResultAttribute.values()) {
			ImmutableSet<ResultAttribute> expectedResultTags = ImmutableSet.of(expectedResultTag);
			InfluxDbWriter writer = getTestInfluxDbWriterWithResultTags(expectedResultTags);
			writer.doWrite(dummyServer(), dummyQuery(), results);

			verify(influxDB, atLeastOnce()).write(messageCaptor.capture());
			BatchPoints batchPoints = messageCaptor.getValue();
			String lineProtocol = batchPoints.getPoints().get(0).lineProtocol();

			assertThat(lineProtocol).contains(enumValueToAttribute(expectedResultTag));
			EnumSet<ResultAttribute> unexpectedResultTags = EnumSet.complementOf(EnumSet.of(expectedResultTag));
			for (ResultAttribute unexpectedResultTag : unexpectedResultTags) {
				assertThat(lineProtocol).doesNotContain(enumValueToAttribute(unexpectedResultTag));
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
		JsonUtils jsonUtils = injector.getInstance(JsonUtils.class);
		JmxProcess process = jsonUtils.parseProcess(input);
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

	private String enumValueToAttribute(ResultAttribute attribute) {
		String[] split = attribute.name().split("_");
		return StringUtils.lowerCase(split[0]) + WordUtils.capitalizeFully(split[1]);
	}
}