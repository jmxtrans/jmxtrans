package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Zack Radick Date: 1/20/12
 */
public class BaseOutputWriterTests {

	private BaseOutputWriter outputWriter;
	private Map<String,Object> settings;

	@Before
	public void setUpOutputWriter() {
		System.setProperty("myHost", "w2");
		System.setProperty("myPort", "123");
		outputWriter = new TestBaseOuputWriter();
		settings = newHashMap();
		settings.put("resolvedHost", "${myHost}");
		settings.put("resolvedPort", "${myPort}");
		outputWriter.setSettings(settings);
	}

	@Test
	public void unsetBooleanSettings() {
		assertThat(outputWriter.getBooleanSetting("bool")).isFalse();
		assertThat(outputWriter.getBooleanSetting("bool", false)).isFalse();
		assertThat(outputWriter.getBooleanSetting("bool", true)).isTrue();
		assertThat(outputWriter.getBooleanSetting("bool", null)).isNull();
	}

	@Test
	public void correctBooleanSettings() {
		settings.put("trueBoolean", true);
		settings.put("falseBoolean", false);
		settings.put("TrueBoolean", TRUE);
		settings.put("FalseBoolean", FALSE);
		assertThat(outputWriter.getBooleanSetting("trueBoolean")).isTrue();
		assertThat(outputWriter.getBooleanSetting("falseBoolean")).isFalse();
		assertThat(outputWriter.getBooleanSetting("TrueBoolean")).isTrue();
		assertThat(outputWriter.getBooleanSetting("FalseBoolean")).isFalse();
	}

	@Test
	public void correctBooleanSettingsWithDefault() {
		settings.put("trueBoolean", true);
		settings.put("falseBoolean", false);
		settings.put("TrueBoolean", TRUE);
		settings.put("FalseBoolean", FALSE);
		assertThat(outputWriter.getBooleanSetting("trueBoolean", null)).isTrue();
		assertThat(outputWriter.getBooleanSetting("falseBoolean", true)).isFalse();
		assertThat(outputWriter.getBooleanSetting("TrueBoolean", false)).isTrue();
		assertThat(outputWriter.getBooleanSetting("FalseBoolean", TRUE)).isFalse();
	}

	@Test
	public void incorrectBooleanSettings() {
		settings.put("Truue", "Truue");
		settings.put("F", "F");
		settings.put("1", "1");
		assertThat(outputWriter.getBooleanSetting("Truue")).isFalse();
		assertThat(outputWriter.getBooleanSetting("F")).isFalse();
		assertThat(outputWriter.getBooleanSetting("1")).isFalse();
	}

	@Test
	public void wrongTypeBooleanSettings() {
		settings.put("Integer", 1);
		settings.put("Long", 0);
		assertThat(outputWriter.getBooleanSetting("Integer")).isFalse();
		assertThat(outputWriter.getBooleanSetting("Long")).isFalse();
	}

	@Test
	public void nullBooleanSettings() {
		settings.put("null", null);
		assertThat(outputWriter.getBooleanSetting("null")).isFalse();
	}

	@Test
	public void unsetIntegerSettings() {
		assertThat(outputWriter.getIntegerSetting("int", 1)).isEqualTo(1);
		assertThat(outputWriter.getIntegerSetting("int", null)).isNull();
	}

	@Test
	public void correctIntegerSettings() {
		settings.put("1", 1);
		assertThat(outputWriter.getIntegerSetting("1", null)).isEqualTo(1);
		assertThat(outputWriter.getIntegerSetting("1", 2)).isEqualTo(1);
	}

	@Test
	public void propertyResolvedIntegerSettings() {
		assertThat(outputWriter.getIntegerSetting("resolvedPort", 0)).isEqualTo(123);
	}

	@Test
	public void typeConvertedIntegerSettings() {
		settings.put("double", 1.1d);
		assertThat(outputWriter.getIntegerSetting("double", null)).isEqualTo(1);
		assertThat(outputWriter.getIntegerSetting("double", 3)).isEqualTo(1);
	}

	@Test
	public void parsedFromStringIntegerSettings() {
		settings.put("string", "123");
		assertThat(outputWriter.getIntegerSetting("string", null)).isEqualTo(123);
		assertThat(outputWriter.getIntegerSetting("string", 3)).isEqualTo(123);
	}

	@Test
	public void parsedFromInvalidStringIntegerSettings() {
		settings.put("string", "NOT_AN_INT");
		settings.put("doubleString", "1.1");
		settings.put("emptyString", "");
		assertThat(outputWriter.getIntegerSetting("string", null)).isNull();
		assertThat(outputWriter.getIntegerSetting("string", 3)).isEqualTo(3);
		assertThat(outputWriter.getIntegerSetting("doubleString", null)).isNull();
		assertThat(outputWriter.getIntegerSetting("doubleString", 3)).isEqualTo(3);
		assertThat(outputWriter.getIntegerSetting("emptyString", null)).isNull();
		assertThat(outputWriter.getIntegerSetting("emptyString", 3)).isEqualTo(3);
	}

	@Test
	public void nullIntegerSettings() {
		settings.put("null", null);
		assertThat(outputWriter.getIntegerSetting("null", null)).isNull();
		assertThat(outputWriter.getIntegerSetting("null", 3)).isEqualTo(3);
	}

	@Test
	public void unsetIntSettings() {
		assertThat(outputWriter.getIntSetting("int", 1)).isEqualTo(1);
	}

	@Test
	public void correctIntSettings() {
		settings.put("1", 1);
		assertThat(outputWriter.getIntSetting("1", 2)).isEqualTo(1);
	}

	@Test
	public void propertyResolvedIntSettings() {
		assertThat(outputWriter.getIntSetting("resolvedPort", 0)).isEqualTo(123);
	}

	// FIXME behaviour is incoherent with the way getIntegerSettings() works
	@Test(expected = IllegalArgumentException.class)
	public void typeConvertedIntSettings() {
		settings.put("double", 1.1d);
		assertThat(outputWriter.getIntSetting("double", 3)).isEqualTo(1);
	}

	@Test
	public void parsedFromStringIntSettings() {
		settings.put("string", "123");
		assertThat(outputWriter.getIntSetting("string", 3)).isEqualTo(123);
	}

	// FIXME behaviour is incoherent with the way getIntegerSettings() works
	@Test(expected = IllegalArgumentException.class)
	public void parsedFromInvalidStringIntSettings() {
		settings.put("string", "NOT_AN_INT");
		settings.put("doubleString", "1.1");
		settings.put("emptyString", "");
		assertThat(outputWriter.getIntSetting("string", 3)).isEqualTo(3);
		assertThat(outputWriter.getIntSetting("doubleString", 3)).isEqualTo(3);
		assertThat(outputWriter.getIntSetting("emptyString", 3)).isEqualTo(3);
	}

	// FIXME behaviour is incoherent with the way getIntegerSettings() works
	@Test(expected = IllegalArgumentException.class)
	public void nullIntSettings() {
		settings.put("null", null);
		assertThat(outputWriter.getIntSetting("null", 3)).isEqualTo(3);
	}

	@Test
	public void unsetStringSettings() {
		assertThat(outputWriter.getStringSetting("string", "")).isEqualTo("");
		assertThat(outputWriter.getStringSetting("string", "NOT_SET")).isEqualTo("NOT_SET");
		assertThat(outputWriter.getStringSetting("string", null)).isNull();
	}

	@Test
	public void standardStringSettings() {
		settings.put("string", "string");
		assertThat(outputWriter.getStringSetting("string", null)).isEqualTo("string");
		assertThat(outputWriter.getStringSetting("string", "other")).isEqualTo("string");
	}

	@Test
	public void numberParsedAsStringSettings() {
		settings.put("integer", 1);
		settings.put("long", 2L);
		assertThat(outputWriter.getStringSetting("integer", null)).isEqualTo("1");
		assertThat(outputWriter.getStringSetting("long", null)).isEqualTo("2");
	}

	@Test
	public void propertyResolvedStringSettings() {
		assertThat(outputWriter.getStringSetting("resolvedHost", "")).isEqualTo("w2");
		assertThat(outputWriter.getStringSetting("resolvedPort", "")).isEqualTo("123");
	}

	@Test
	public void nullStringSettings() {
		settings.put("null", null);
		assertThat(outputWriter.getStringSetting("null", null)).isNull();
		assertThat(outputWriter.getStringSetting("null", "default")).isEqualTo("default");
	}

	@After
	public void removeSystemProperties() {
		System.clearProperty("myHost");
		System.clearProperty("myPort");
	}

	private static final class TestBaseOuputWriter extends BaseOutputWriter {
		@Override
		public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
			throw new UnsupportedOperationException("doWrite() not implemented for TestBaseOutputWriter.");
		}

		@Override
		public void validateSetup(Server server, Query query) throws ValidationException {
			throw new UnsupportedOperationException("validateSetup() not implemented for TestBaseOutputWriter.");
		}
	}
}
