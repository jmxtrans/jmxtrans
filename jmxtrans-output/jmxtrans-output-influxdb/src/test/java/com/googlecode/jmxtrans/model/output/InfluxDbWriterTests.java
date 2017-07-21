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
import org.mockito.verification.VerificationMode;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ServerFixtures.DEFAULT_PORT;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.googlecode.jmxtrans.model.output.InfluxDbWriter.JMX_PORT_KEY;
import static com.googlecode.jmxtrans.model.output.InfluxDbWriter.TAG_HOSTNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
	private static final ImmutableSet<ResultAttribute> DEFAULT_RESULT_ATTRIBUTES = ImmutableSet.copyOf(ResultAttributes.values());

	Result result = new Result(2l, "attributeName", "className", "objDomain", "keyAlias", "type=test,name=name",
			ImmutableList.of("key"), 1);
	ImmutableList<Result> results = ImmutableList.of(result);

	@Test
	public void pointsAreWrittenToInfluxDb() throws Exception {
		BatchPoints batchPoints = writeToInfluxDb(getTestInfluxDbWriterWithDefaultSettings());

		// The database name is present
		assertThat(batchPoints.getDatabase()).isEqualTo(DATABASE_NAME);

		// Point only exposes its state via a line protocol so we have to
		// make assertions against this.
		// Format is:
		// measurement,<comma separated key=val tags>" " <comma separated
		// key=val fields>" "time
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
	public void customTagsAreWrittenToDb() throws Exception {
		ImmutableMap<String, String> tags = ImmutableMap.of("customTag", "customValue");
		BatchPoints batchPoints = writeToInfluxDb(getTestInfluxDbWriterWithCustomTags(tags));
		assertThat(batchPoints.getPoints().get(0).lineProtocol()).contains("customTag=customValue");
	}

	@Test
	public void attributeNameIncludesTypeNames() throws Exception {
		ImmutableList<String> typeNames = ImmutableList.of("name");
		BatchPoints batchPoints = writeToInfluxDb(getTestInfluxDbWriterWithTypeNames(typeNames));
		assertThat(batchPoints.getPoints().get(0).lineProtocol()).contains("name.attributeName_key");
	}

	@Test
	public void writeConsistencyLevelsAreAppliedToBatchPointsBeingWritten() throws Exception {
		for (ConsistencyLevel consistencyLevel : ConsistencyLevel.values()) {
			BatchPoints batchPoints = writeAtLeastOnceToInfluxDb(
					getTestInfluxDbWriterWithWriteConsistency(consistencyLevel));
			assertThat(batchPoints.getConsistency()).isEqualTo(consistencyLevel);
		}
	}

	@Test
	public void onlyRequestedResultPropertiesAreAppliedAsTags() throws Exception {
		for (ResultAttribute expectedResultTag : ResultAttributes.values()) {
			ImmutableSet<ResultAttribute> expectedResultTags = ImmutableSet.of(expectedResultTag);
			BatchPoints batchPoints = writeAtLeastOnceToInfluxDb(getTestInfluxDbWriterWithResultTags(expectedResultTags));
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
		writeToInfluxDb(getTestInfluxDbWriterWithDefaultSettings());
		verify(influxDB).createDatabase(DATABASE_NAME);
	}

	@Test
	public void databaseIsNotCreated() throws Exception {
		writeToInfluxDb(getTestInfluxDbWriterNoDatabaseCreation());
		verify(influxDB, never()).createDatabase(anyString());
	}

	@Test
	public void jmxPortIsReportedAsFieldByDefault() throws Exception {
		BatchPoints batchPoints = writeToInfluxDb(getTestInfluxDbWriterWithDefaultSettings());
		verifyJMXPortOnlyInToken(batchPoints.getPoints().get(0).lineProtocol(), 1 /*Fields token*/);
	}

	@Test
	public void jmxPortIsReportedAsTag() throws Exception {
		BatchPoints batchPoints = writeToInfluxDb(getTestInfluxDbWriterWithReportJmxPortAsTag());
		verifyJMXPortOnlyInToken(batchPoints.getPoints().get(0).lineProtocol(), 0 /*Tags token*/);
	}

	private void verifyJMXPortOnlyInToken(String lineProtocol, int tokenContainingJMXPort) {
		// lineProtocol is from Point.lineProtocol() :
		// The format is
		//  measurement,<comma separated key=val tags>" " <comma separated key=val fields>" "time
		// So splitted by space we have three tokens : the tags are in the first one ([0])
		// and the fields are in the second one ([1])
		// so we check between  the 0 and 1 indexes (tag and fields)
		String[] protocolTokens = lineProtocol.split(" ");
		assertThat(protocolTokens).hasSize(3);
		assertThat(protocolTokens[tokenContainingJMXPort]).contains(JMX_PORT_KEY + "=" + DEFAULT_PORT);
		assertThat(protocolTokens[1 - tokenContainingJMXPort]).doesNotContain(JMX_PORT_KEY);
	}

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		File input = new File(InfluxDbWriterTests.class.getResource("/influxDB.json").toURI());
		Injector injector = JmxTransModule.createInjector(new JmxTransConfiguration());
		ProcessConfigUtils processConfigUtils = injector.getInstance(ProcessConfigUtils.class);
		JmxProcess process = processConfigUtils.parseProcess(input);
		assertThat(process.getName()).isEqualTo("influxDB.json");
	}

	private BatchPoints writeToInfluxDb(InfluxDbWriter writer) throws Exception {
		return writeToInfluxDb(writer, times(1));
	}

	private BatchPoints writeAtLeastOnceToInfluxDb(InfluxDbWriter writer) throws Exception {
		return writeToInfluxDb(writer, atLeastOnce());
	}

	private BatchPoints writeToInfluxDb(InfluxDbWriter writer, VerificationMode mode) throws Exception {
		writer.doWrite(dummyServer(), dummyQuery(), results);
		verify(influxDB, mode).write(messageCaptor.capture());
		return messageCaptor.getValue();
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
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES, true, false);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithResultTags(ImmutableSet<ResultAttribute> resultTags) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, resultTags, DEFAULT_TYPE_NAMES, true, false);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithCustomTags(ImmutableMap<String, String> tags) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, tags, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES, true, false);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithTypeNames(ImmutableList<String> typeNames) {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, typeNames, true, false);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithWriteConsistency(ConsistencyLevel consistencyLevel) {
		return getTestInfluxDbWriter(consistencyLevel, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES, true, false);
	}

	private InfluxDbWriter getTestInfluxDbWriterNoDatabaseCreation() {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES, false, false);
	}

	private InfluxDbWriter getTestInfluxDbWriterWithReportJmxPortAsTag() {
		return getTestInfluxDbWriter(DEFAULT_CONSISTENCY_LEVEL, DEFAULT_RETENTION_POLICY, DEFAULT_CUSTOM_TAGS, DEFAULT_RESULT_ATTRIBUTES, DEFAULT_TYPE_NAMES, true, true);
	}

	private InfluxDbWriter getTestInfluxDbWriter(ConsistencyLevel consistencyLevel, String retentionPolicy, ImmutableMap<String, String> tags,
												 ImmutableSet<ResultAttribute> resultTags, ImmutableList<String> typeNames, boolean createDatabase, boolean reportJmxPortAsTag) {
		return new InfluxDbWriter(influxDB, DATABASE_NAME, consistencyLevel, retentionPolicy, tags, resultTags, typeNames, createDatabase, reportJmxPortAsTag);
	}
}
