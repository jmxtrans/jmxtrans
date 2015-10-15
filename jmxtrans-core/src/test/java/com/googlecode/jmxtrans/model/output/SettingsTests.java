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

import org.junit.Test;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.model.output.Settings.getBooleanSetting;
import static com.googlecode.jmxtrans.model.output.Settings.getIntSetting;
import static com.googlecode.jmxtrans.model.output.Settings.getIntegerSetting;
import static com.googlecode.jmxtrans.model.output.Settings.getStringSetting;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

public class SettingsTests {

	@Test
	public void unsetBooleanSettings() {
		Map<String, Object> settings = newHashMap();

		assertThat(getBooleanSetting(settings, "bool")).isFalse();
		assertThat(getBooleanSetting(settings, "bool", false)).isFalse();
		assertThat(getBooleanSetting(settings, "bool", true)).isTrue();
		assertThat(getBooleanSetting(settings, "bool", null)).isNull();
	}

	@Test
	public void correctBooleanSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("trueBoolean", true);
		settings.put("falseBoolean", false);
		settings.put("TrueBoolean", TRUE);
		settings.put("FalseBoolean", FALSE);

		assertThat(getBooleanSetting(settings, "trueBoolean")).isTrue();
		assertThat(getBooleanSetting(settings, "falseBoolean")).isFalse();
		assertThat(getBooleanSetting(settings, "TrueBoolean")).isTrue();
		assertThat(getBooleanSetting(settings, "FalseBoolean")).isFalse();
	}

	@Test
	public void correctBooleanSettingsWithDefault() {
		Map<String, Object> settings = newHashMap();
		settings.put("trueBoolean", true);
		settings.put("falseBoolean", false);
		settings.put("TrueBoolean", TRUE);
		settings.put("FalseBoolean", FALSE);

		assertThat(getBooleanSetting(settings, "trueBoolean", null)).isTrue();
		assertThat(getBooleanSetting(settings, "falseBoolean", true)).isFalse();
		assertThat(getBooleanSetting(settings, "TrueBoolean", false)).isTrue();
		assertThat(getBooleanSetting(settings, "FalseBoolean", TRUE)).isFalse();
	}

	@Test
	public void incorrectBooleanSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("Truue", "Truue");
		settings.put("F", "F");
		settings.put("1", "1");

		assertThat(getBooleanSetting(settings, "Truue")).isFalse();
		assertThat(getBooleanSetting(settings, "F")).isFalse();
		assertThat(getBooleanSetting(settings, "1")).isFalse();
	}

	@Test
	public void wrongTypeBooleanSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("Integer", 1);
		settings.put("Long", 0);

		assertThat(getBooleanSetting(settings, "Integer")).isFalse();
		assertThat(getBooleanSetting(settings, "Long")).isFalse();
	}

	@Test
	public void unsetIntegerSettings() {
		Map<String, Object> settings = newHashMap();

		assertThat(getIntegerSetting(settings, "int", 1)).isEqualTo(1);
		assertThat(getIntegerSetting(settings, "int", null)).isNull();
	}

	@Test
	public void correctIntegerSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("1", 1);

		assertThat(getIntegerSetting(settings, "1", null)).isEqualTo(1);
		assertThat(getIntegerSetting(settings, "1", 2)).isEqualTo(1);
	}

	@Test
	public void typeConvertedIntegerSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("double", 1.1d);

		assertThat(getIntegerSetting(settings, "double", null)).isEqualTo(1);
		assertThat(getIntegerSetting(settings, "double", 3)).isEqualTo(1);
	}

	@Test
	public void parsedFromStringIntegerSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("string", "123");

		assertThat(getIntegerSetting(settings, "string", null)).isEqualTo(123);
		assertThat(getIntegerSetting(settings, "string", 3)).isEqualTo(123);
	}

	@Test
	public void parsedFromInvalidStringIntegerSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("string", "NOT_AN_INT");
		settings.put("doubleString", "1.1");
		settings.put("emptyString", "");

		assertThat(getIntegerSetting(settings, "string", null)).isNull();
		assertThat(getIntegerSetting(settings, "string", 3)).isEqualTo(3);
		assertThat(getIntegerSetting(settings, "doubleString", null)).isNull();
		assertThat(getIntegerSetting(settings, "doubleString", 3)).isEqualTo(3);
		assertThat(getIntegerSetting(settings, "emptyString", null)).isNull();
		assertThat(getIntegerSetting(settings, "emptyString", 3)).isEqualTo(3);
	}

	@Test
	public void unsetIntSettings() {
		Map<String, Object> settings = newHashMap();

		assertThat(getIntSetting(settings, "int", 1)).isEqualTo(1);
	}

	@Test
	public void correctIntSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("1", 1);

		assertThat(getIntSetting(settings, "1", 2)).isEqualTo(1);
	}

	// FIXME behaviour is incoherent with the way getIntegerSettings() works
	@Test(expected = IllegalArgumentException.class)
	public void typeConvertedIntSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("double", 1.1d);

		assertThat(getIntSetting(settings, "double", 3)).isEqualTo(1);
	}

	@Test
	public void parsedFromStringIntSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("string", "123");

		assertThat(getIntSetting(settings, "string", 3)).isEqualTo(123);
	}

	// FIXME behaviour is incoherent with the way getIntegerSettings() works
	@Test(expected = IllegalArgumentException.class)
	public void parsedFromInvalidStringIntSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("string", "NOT_AN_INT");
		settings.put("doubleString", "1.1");
		settings.put("emptyString", "");

		assertThat(getIntSetting(settings, "string", 3)).isEqualTo(3);
		assertThat(getIntSetting(settings, "doubleString", 3)).isEqualTo(3);
		assertThat(getIntSetting(settings, "emptyString", 3)).isEqualTo(3);
	}

	@Test
	public void unsetStringSettings() {
		Map<String, Object> settings = newHashMap();

		assertThat(getStringSetting(settings, "string", "")).isEqualTo("");
		assertThat(getStringSetting(settings, "string", "NOT_SET")).isEqualTo("NOT_SET");
		assertThat(getStringSetting(settings, "string", null)).isNull();
	}

	@Test
	public void standardStringSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("string", "string");

		assertThat(getStringSetting(settings, "string", null)).isEqualTo("string");
		assertThat(getStringSetting(settings, "string", "other")).isEqualTo("string");
	}

	@Test
	public void numberParsedAsStringSettings() {
		Map<String, Object> settings = newHashMap();
		settings.put("integer", 1);
		settings.put("long", 2L);

		assertThat(getStringSetting(settings, "integer", null)).isEqualTo("1");
		assertThat(getStringSetting(settings, "long", null)).isEqualTo("2");
	}

}
