package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.model.output.Settings.getIntSetting;
import static com.googlecode.jmxtrans.model.output.Settings.getIntegerSetting;
import static com.googlecode.jmxtrans.model.output.Settings.getStringSetting;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Zack Radick Date: 1/20/12
 */
public class BaseOutputWriterTests {

	@Before
	public void setUpOutputWriter() {
		System.setProperty("myHost", "w2");
		System.setProperty("myPort", "123");
	}

	@Test(expected = NullPointerException.class)
	public void cannotHaveNullValueInSettings() {
		BaseOutputWriter outputWriter = new TestBaseOuputWriter();
		Map<String, Object> settings = newHashMap();
		settings.put("null", null);
		outputWriter.setSettings(settings);
	}

	@Test
	public void propertyResolvedIntegerSettings() {
		BaseOutputWriter outputWriter = new TestBaseOuputWriter();
		Map<String, Object> settings = newHashMap();
		settings.put("resolvedPort", "${myPort}");
		outputWriter.setSettings(settings);

		assertThat(getIntegerSetting(outputWriter.getSettings(), "resolvedPort", 0)).isEqualTo(123);
	}

	@Test
	public void propertyResolvedIntSettings() {
		BaseOutputWriter outputWriter = new TestBaseOuputWriter();
		Map<String, Object> settings = newHashMap();
		settings.put("resolvedPort", "${myPort}");
		outputWriter.setSettings(settings);

		assertThat(getIntSetting(outputWriter.getSettings(), "resolvedPort", 0)).isEqualTo(123);
	}

	@Test
	public void propertyResolvedStringSettings() {
		BaseOutputWriter outputWriter = new TestBaseOuputWriter();
		Map<String, Object> settings = newHashMap();
		settings.put("resolvedHost", "${myHost}");
		settings.put("resolvedPort", "${myPort}");
		outputWriter.setSettings(settings);

		assertThat(getStringSetting(outputWriter.getSettings(), "resolvedHost", "")).isEqualTo("w2");
		assertThat(getStringSetting(outputWriter.getSettings(), "resolvedPort", "")).isEqualTo("123");
	}

	@Test
	public void booleanValuesAreTransformedToNumber() throws Exception {
		final ArrayList<Result> processedResults = Lists.newArrayList();
		BaseOutputWriter outputWriter = new BaseOutputWriter(
				ImmutableList.<String>of(), true, false, Maps.<String, Object>newHashMap()) {
			@Override
			protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
				processedResults.addAll(results);
			}
			@Override
			public void validateSetup(Server server, Query query) throws ValidationException {
			}
		};
		Result result = new Result(0, "", "", "", "", "", ImmutableMap.<String, Object>of("true", true, "false", false));
		outputWriter.doWrite(null, null, ImmutableList.of(result));

		assertThat(processedResults).hasSize(1);
		Result processedResult = processedResults.get(0);
		assertThat(processedResult.getValues().get("true")).isEqualTo(1);
		assertThat(processedResult.getValues().get("false")).isEqualTo(0);
	}

	@Test
	public void booleanValuesAreNotTransformedToNumber() throws Exception {
		final ArrayList<Result> processedResults = Lists.newArrayList();
		BaseOutputWriter outputWriter = new BaseOutputWriter(
				ImmutableList.<String>of(), false, false, Maps.<String, Object>newHashMap()) {
			@Override
			protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
				processedResults.addAll(results);
			}
			@Override
			public void validateSetup(Server server, Query query) throws ValidationException {
			}
		};
		Result result = new Result(0, "", "", "", "", "", ImmutableMap.<String, Object>of("true", true, "false", false));
		outputWriter.doWrite(null, null, ImmutableList.of(result));

		assertThat(processedResults).hasSize(1);
		Result processedResult = processedResults.get(0);
		assertThat(processedResult.getValues().get("true")).isEqualTo(true);
		assertThat(processedResult.getValues().get("false")).isEqualTo(false);
	}

	@After
	public void removeSystemProperties() {
		System.clearProperty("myHost");
		System.clearProperty("myPort");
	}

	private static final class TestBaseOuputWriter extends BaseOutputWriter {
		public TestBaseOuputWriter() {
			super(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap());
		}

		@Override
		public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
			throw new UnsupportedOperationException("doWrite() not implemented for TestBaseOutputWriter.");
		}

		@Override
		public void validateSetup(Server server, Query query) throws ValidationException {
			throw new UnsupportedOperationException("validateSetup() not implemented for TestBaseOutputWriter.");
		}
	}
}
