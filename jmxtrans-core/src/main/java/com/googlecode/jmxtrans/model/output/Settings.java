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

import java.util.Map;

// FIXME settings parsing is very tolerant. It will try its best to return
// default values, silently ignoring parsing errors. Except in the case of
// primitive int settings, where is raises an IllegalArgumentException. We
// should resolve this incoherence, probably by always raising an exception
// when the setting cannot be parsed.
// FIXME resolution of settings (via PropertyResolver.resolveMap()) is not
// consistent. Settings added by other means than setSettings() are never
// resolved.
public final class Settings {
	private Settings() {}

	/**
	 * Returns a boolean value for the key, defaults to false if not specified.
	 * This is equivalent to calling {@link #getBooleanSetting(java.util.Map, String, Boolean)}.
	 *
	 *
	 * @param settings
	 * @param key the key value to get the boolean setting for
	 * @return the value set for the key, defaults to false
	 */
	public static Boolean getBooleanSetting(Map<String, Object> settings, String key) {
		return getBooleanSetting(settings, key, false);
	}

	/**
	 * Gets a Boolean value for the key, returning the default value given if
	 * not specified or not a valid boolean value.
	 *
	 *
	 * @param settings
	 * @param key        the key to get the boolean setting for
	 * @param defaultVal the default value to return if the setting was not specified
	 *                   or was not coercible to a Boolean value
	 * @return the Boolean value for the setting
	 */
	public static Boolean getBooleanSetting(Map<String, Object> settings, String key, Boolean defaultVal) {
		final Object value = settings.get(key);

		if (value == null) {
			return defaultVal;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			return Boolean.valueOf((String) value);
		}
		return defaultVal;
	}

	/**
	 * Gets an Integer value for the key, returning the default value given if
	 * not specified or not a valid numeric value.
	 *
	 *
	 * @param settings
	 * @param key        the key to get the Integer setting for
	 * @param defaultVal the default value to return if the setting was not specified
	 *                   or was not coercible to an Integer value
	 * @return the Integer value for the setting
	 */
	public static Integer getIntegerSetting(Map<String, Object> settings, String key, Integer defaultVal) {
		final Object value = settings.get(key);
		if (value == null) {
			return defaultVal;
		}

		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return defaultVal;
			}
		}
		return defaultVal;
	}

	/**
	 * Gets a String value for the setting, returning the default value if not
	 * specified.
	 *
	 *
	 * @param settings
	 * @param key        the key to get the String setting for
	 * @param defaultVal the default value to return if the setting was not specified
	 * @return the String value for the setting
	 */
	public static String getStringSetting(Map<String, Object> settings, String key, String defaultVal) {
		final Object value = settings.get(key);
		return value != null ? value.toString() : defaultVal;
	}

	/**
	 * Gets an int value for the setting, returning the default value if not
	 * specified.
	 *
	 *
	 * @param settings
	 * @param key        the key to get the int value for
	 * @param defaultVal default value if the setting was not specified
	 * @return the int value for the setting
	 * @throws IllegalArgumentException if setting does not contain an int
	 */
	protected static int getIntSetting(Map<String, Object> settings, String key, int defaultVal) throws IllegalArgumentException {
		if (settings.containsKey(key)) {
			final Object objectValue = settings.get(key);
			if (objectValue == null) {
				throw new IllegalArgumentException("Setting '" + key + " null");
			}
			final String value = objectValue.toString();
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				throw new IllegalArgumentException("Setting '" + key + "=" + value + "' is not an integer");
			}
		} else {
			return defaultVal;
		}
	}
}
