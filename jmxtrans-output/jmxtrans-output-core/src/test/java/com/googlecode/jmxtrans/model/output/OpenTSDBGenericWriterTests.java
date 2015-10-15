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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


/**
 * Tests for {@link OpenTSDBGenericWriter}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenTSDBGenericWriter.class, InetAddress.class})
public class OpenTSDBGenericWriterTests {

	protected Query mockQuery;
	protected Result mockResult;

	// Interactions with the custom, test subclass of OpenTSDBGenericWriter.
	protected boolean tvAddHostnameTagDefault;
	protected boolean prepareSenderCalled;
	protected boolean shutdownSenderCalled;
	protected boolean startOutputCalled;
	protected boolean finishOutputCalled;
	protected List<String> tvMetricLinesSent;

	@Before
	public void setupTest() {
		this.mockQuery = Mockito.mock(Query.class);
		this.mockResult = Mockito.mock(Result.class);

		// Setup test data
		tvAddHostnameTagDefault = true;
		prepareSenderCalled = false;
		shutdownSenderCalled = false;
		startOutputCalled = false;
		finishOutputCalled = false;
		tvMetricLinesSent = new LinkedList<String>();

		// Setup common mock interactions.
		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("x-att1-x", (Object) "120021"));
		Mockito.when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		Mockito.when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		Mockito.when(this.mockResult.getTypeName()).
				thenReturn("Type=x-type-x,Group=x-group-x,Other=x-other-x,Name=x-name-x");

	}

	@Test
	public void testMergedTypeNameValues1() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();
		// Verify the default is the same as the TRUE path.
		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), true);
	}

	@Test
	public void testMergedTypeNameValues2() throws Exception {
		OpenTSDBGenericWriter writer = createWriter("mergeTypeNamesTags", TRUE);

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), true);
	}

	@Test
	public void testMergedTypeNameValues3() throws Exception {
		// Verify the FALSE path.
		OpenTSDBGenericWriter writer = createWriter("mergeTypeNamesTags", FALSE);

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), false);
	}

	@Test
	public void testEmptyTagSetting() throws Exception {
		Map<String, String> tagMap = newHashMap();
		Map<String, Object> settings = newHashMap();
		settings.put("tags", tagMap);
		OpenTSDBGenericWriter writer = new TestOpenTSDBGenericWriter(
				ImmutableList.<String>of(),
				false,
				settings);

		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("X-ATT-X", (Object) "120021"));

		// Verify empty tag map.
		Assertions.assertThat(writer.getTypeNames()).isEmpty();

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertTrue(
				this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021 host=[^ ]*$"));
	}

	@Test
	public void testTagSetting() throws Exception {
		Map<String, String> tagMap;

		// Verify tag map with multiple values.
		tagMap = newHashMap();
		tagMap.put("x-tag1-x", "x-tag1val-x");
		tagMap.put("x-tag2-x", "x-tag2val-x");
		tagMap.put("x-tag3-x", "x-tag3val-x");
		OpenTSDBGenericWriter writer = createWriter("tags", tagMap);

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));
		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag1-x=x-tag1val-x\\b.*"));
		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag2-x=x-tag2val-x\\b.*"));
		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bx-tag3-x=x-tag3val-x\\b.*"));
	}

	@Test
	public void testAddHostnameTag() throws Exception {
		OpenTSDBGenericWriter writer = createWriter("mergeTypeNamesTags", TRUE);

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertEquals(1, tvMetricLinesSent.size());
		validateMergedTypeNameValues(tvMetricLinesSent.get(0), true);
		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches(".*host=.*"));
	}

	@Test
	public void testDontAddHostnameTag() throws Exception {
		OpenTSDBGenericWriter writer = createWriter("addHostnameTag", false);

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertFalse(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));
	}

	@Test
	public void testEmptyResultValues() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();

		ImmutableMap<String, Object> values = ImmutableMap.of();
		Mockito.when(this.mockResult.getValues()).thenReturn(values);

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertEquals(0, this.tvMetricLinesSent.size());
	}

	@Test
	public void testOneValueMatchingAttribute() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();

		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("X-ATT-X", (Object) "120021"));

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));
		Assert.assertFalse(this.tvMetricLinesSent.get(0).matches(".*\\btype=.*"));
	}

	@Test
	public void testMultipleValuesWithMatchingAttribute() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();

		Mockito.when(this.mockResult.getValues()).
				thenReturn(ImmutableMap.of("X-ATT-X", (Object) "120021", "XX-ATT-XX", (Object) "210012"));

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();
		Assert.assertEquals(2, this.tvMetricLinesSent.size());

		String xLine;
		String xxLine;
		if (this.tvMetricLinesSent.get(0).contains("XX-ATT-XX")) {
			xxLine = this.tvMetricLinesSent.get(0);
			xLine = this.tvMetricLinesSent.get(1);
		} else {
			xLine = this.tvMetricLinesSent.get(0);
			xxLine = this.tvMetricLinesSent.get(1);
		}

		Assert.assertTrue(xLine.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
		Assert.assertTrue(xLine.matches(".*\\btype=X-ATT-X\\b.*"));

		Assert.assertTrue(xxLine.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 210012.*"));
		Assert.assertTrue(xxLine.matches(".*\\btype=XX-ATT-XX\\b.*"));
	}

	@Test
	public void testNonNumericValue() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();

		Mockito.when(this.mockResult.getValues()).thenReturn(ImmutableMap.of("X-ATT-X", (Object) "THIS-IS-NOT-A-NUMBER"));

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertEquals(0, this.tvMetricLinesSent.size());
	}

	@Test
	public void testJexlNaming() throws Exception {
		OpenTSDBGenericWriter writer = createWriter("metricNamingExpression", "'xx-jexl-constant-name-xx'");

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();

		Assert.assertTrue(this.tvMetricLinesSent.get(0).matches("^xx-jexl-constant-name-xx 0 120021.*"));
	}

	@Test(expected = LifecycleException.class)
	public void testInvalidJexlNaming() throws Exception {
		OpenTSDBGenericWriter writer = createWriter("metricNamingExpression", "invalid expression here");

		writer.start();
	}

	@Test
	public void testDebugOuptutResultString() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();
		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();
	}

	@Test
	public void testValidateValidHostPort() throws Exception {
		OpenTSDBGenericWriter writer = createWriter(ImmutableMap.of(
				"host", (Object) "localhost",
				"port", 4242));

		writer.start();
		writer.validateSetup(null, this.mockQuery);
	}

	@Test
	public void testPortNumberAsString() throws Exception {
		OpenTSDBGenericWriter writer = createWriter(ImmutableMap.of("host", (Object) "localhost", "port", "4242"));

		writer.start();
		writer.validateSetup(null, this.mockQuery);
	}

	@Test
	public void testDefaultHookMethods() throws Exception {
		OpenTSDBGenericWriter writer = new MinimalTestOpenTSDBGenericWriter(
				ImmutableList.<String>of(),
				false,
				Collections.<String, Object>emptyMap());

		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		writer.stop();
	}

	@Test
	public void testHooksCalled() throws Exception {
		OpenTSDBGenericWriter writer = createWriter();
		writer.start();
		Assert.assertTrue(prepareSenderCalled);
		Assert.assertFalse(shutdownSenderCalled);
		Assert.assertFalse(startOutputCalled);
		Assert.assertFalse(finishOutputCalled);

		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		Assert.assertTrue(prepareSenderCalled);
		Assert.assertFalse(shutdownSenderCalled);
		Assert.assertTrue(startOutputCalled);
		Assert.assertTrue(finishOutputCalled);

		writer.stop();
		Assert.assertTrue(prepareSenderCalled);
		Assert.assertTrue(shutdownSenderCalled);
		Assert.assertTrue(startOutputCalled);
		Assert.assertTrue(finishOutputCalled);

	}

	@Test
	public void testDebugEanbled() throws Exception {
		OpenTSDBGenericWriter writer = createWriter(ImmutableMap.of("host", (Object) "localhost", "port", 4242, "debug", true));

		writer.start();
		writer.validateSetup(null, this.mockQuery);
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
	}

	@Test
	public void testLocalhostUnknownHostException() throws Exception {
		UnknownHostException unknownHostException = new UnknownHostException("X-TEST-UHE-X");
		try {
			PowerMockito.mockStatic(InetAddress.class);
			PowerMockito.when(InetAddress.getLocalHost()).thenThrow(unknownHostException);

			OpenTSDBGenericWriter writer = createWriter();

			Assert.fail("LifecycleException missing");
		} catch (UnknownHostException ex) {
			// Verify.
			Assert.assertSame(unknownHostException, ex);
		}
	}

	/**
	 * Confirm operation when the host tag is enabled, but the local hostname is not known.
	 */
	@Test
	public void testNullHostTagname() throws Exception {
		// Prepare.
		InetAddress mockInetAddress = Mockito.mock(InetAddress.class);
		PowerMockito.mockStatic(InetAddress.class);
		PowerMockito.when(InetAddress.getLocalHost()).thenReturn(mockInetAddress);
		Mockito.when(mockInetAddress.getHostName()).thenReturn(null);
		OpenTSDBGenericWriter writer = createWriter("addHostnameTag", true);


		// Execute.
		writer.start();
		writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));

		// Validate.
		Assert.assertFalse(this.tvMetricLinesSent.get(0).matches(".*\\bhost=.*"));    // Ensure host tag is excluded
	}

	protected OpenTSDBGenericWriter createWriter() throws LifecycleException, UnknownHostException {
		return createWriter(Collections.<String, Object>emptyMap());
	}

	protected OpenTSDBGenericWriter createWriter(String additionalSettingKey, Object additionalSettingValue) throws LifecycleException, UnknownHostException {
		return createWriter(ImmutableMap.of(additionalSettingKey, additionalSettingValue));
	}

	protected OpenTSDBGenericWriter createWriter(Map<String, Object> additionalSettings) throws LifecycleException, UnknownHostException {
		Map<String, Object> settings = newHashMap();
		settings.put("host", "localhost");
		settings.put("port", 4242);
		settings.putAll(additionalSettings);
		OpenTSDBGenericWriter writer = new TestOpenTSDBGenericWriter(
				ImmutableList.of("Type", "Group", "Name", "Missing"),
				false,
				settings);
		return writer;
	}

	protected void validateMergedTypeNameValues(String resultString, boolean mergedInd) {
		if (mergedInd) {
			Assert.assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
			Assert.assertTrue(resultString.matches(".*\\btype=x-att1-x\\b.*"));
			Assert.assertTrue(resultString.matches(".*\\bTypeGroupNameMissing=x-type-x_x-group-x_x-name-x\\b.*"));
		} else {
			Assert.assertTrue(resultString.matches("^X-DOMAIN.PKG.CLASS-X\\.X-ATT-X 0 120021.*"));
			Assert.assertTrue(resultString.matches(".*\\btype=x-att1-x\\b.*"));
			Assert.assertTrue(resultString.matches(".*\\bType=x-type-x\\b.*"));
			Assert.assertTrue(resultString.matches(".*\\bGroup=x-group-x\\b.*"));
			Assert.assertTrue(resultString.matches(".*\\bName=x-name-x\\b.*"));
			Assert.assertTrue(resultString.matches(".*\\bMissing=(\\s.*|$)"));
		}
	}

	private class TestOpenTSDBGenericWriter extends OpenTSDBGenericWriter {

		public TestOpenTSDBGenericWriter(
				@JsonProperty("typeNames") ImmutableList<String> typeNames,
				@JsonProperty("debug") Boolean debugEnabled,
				@JsonProperty("settings") Map<String, Object> settings) throws LifecycleException, UnknownHostException {
			super(typeNames,  false, debugEnabled, "localhost", 1234, null, null, null, null, null, settings);
		}

		protected void prepareSender() throws LifecycleException {
			OpenTSDBGenericWriterTests.this.prepareSenderCalled = true;
		}

		protected void shutdownSender() throws LifecycleException {
			OpenTSDBGenericWriterTests.this.shutdownSenderCalled = true;
		}

		protected void startOutput() throws IOException {
			OpenTSDBGenericWriterTests.this.startOutputCalled = true;
		}

		protected void finishOutput() throws IOException {
			OpenTSDBGenericWriterTests.this.finishOutputCalled = true;
		}

		protected boolean getAddHostnameTagDefault() {
			return tvAddHostnameTagDefault;
		}

		protected void sendOutput(String metricLine) {
			OpenTSDBGenericWriterTests.this.tvMetricLinesSent.add(metricLine);
		}
	}

	private class MinimalTestOpenTSDBGenericWriter extends OpenTSDBGenericWriter {
		public MinimalTestOpenTSDBGenericWriter(
				@JsonProperty("typeNames") ImmutableList<String> typeNames,
				@JsonProperty("debug") Boolean debugEnabled,
				@JsonProperty("settings") Map<String, Object> settings) throws LifecycleException, UnknownHostException {
			super(typeNames, false, debugEnabled, "localhost", 1234, null, null, null, null, null, settings);
		}

		protected boolean getAddHostnameTagDefault() {
			return tvAddHostnameTagDefault;
		}

		protected void sendOutput(String metricLine) {
			tvMetricLinesSent.add(metricLine);
		}
	}
}
