package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.PropertyResolver;
import com.googlecode.jmxtrans.util.StringUtils;

/**
 * Implements the common code for output filters.
 *
 * @author jon
 */
public abstract class BaseOutputWriter implements OutputWriter {

	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String TEMPLATE_FILE = "templateFile";
	public static final String BINARY_PATH = "binaryPath";
	public static final String DEBUG = "debug";
	public static final String TYPE_NAMES = "typeNames";

	private Boolean debugEnabled = null;
	private Map<String, Object> settings;

	/** */
	public void addSetting(String key, Object value) {
		getSettings().put(key, value);
	}

	/** */
	public Map<String, Object> getSettings() {
		if (this.settings == null) {
			this.settings = new TreeMap<String, Object>();
		}
		return this.settings;
	}

	/** */
	public void setSettings(Map<String, Object> settings) {
		this.settings = settings;
		PropertyResolver.resolveMap(this.settings);
	}

	/**
	 * Returns a boolean value for the key, defaults to false if not specified.
	 * This is equivalent to calling {@link #getBooleanSetting(String, Boolean)
	 * getBooleanSetting(key,false)}.
	 *
	 * @param key the key value to get the boolean setting for
	 * @return the value set for the key, defaults to false
	 */
	public Boolean getBooleanSetting(String key) {
		return getBooleanSetting(key, false);
	}

	/**
	 * Gets a Boolean value for the key, returning the default value given if
	 * not specified or not a valid boolean value.
	 *
	 * @param key        the key to get the boolean setting for
	 * @param defaultVal the default value to return if the setting was not specified
	 *                   or was not coercible to a Boolean value
	 * @return the Boolean value for the setting
	 */
	public Boolean getBooleanSetting(String key, Boolean defaultVal) {
		Boolean result = null;
		final Object value = this.getSettings().get(key);
		if (value != null) {
			if (value instanceof Boolean) {
				result = (Boolean) value;
			} else if (value instanceof String) {
				result = Boolean.valueOf((String) value);
			}
		}
		return result != null ? result : defaultVal;
	}

	/**
	 * Gets an Integer value for the key, returning the default value given if
	 * not specified or not a valid numeric value.
	 *
	 * @param key        the key to get the Integer setting for
	 * @param defaultVal the default value to return if the setting was not specified
	 *                   or was not coercible to an Integer value
	 * @return the Integer value for the setting
	 */
	public Integer getIntegerSetting(String key, Integer defaultVal) {
		Integer result = null;
		final Object value = this.getSettings().get(key);
		if (value != null) {
			if (value instanceof Number) {
				result = ((Number) value).intValue();
			} else if (value instanceof String) {
				try {
					result = Integer.parseInt((String) value);
				} catch (NumberFormatException e) {
					// An Integer value could not be coerced from the String, we
					// will return the default
				}
			}
		}
		return result != null ? result : defaultVal;
	}

	/**
	 * Gets a String value for the setting, returning the default value if not
	 * specified.
	 *
	 * @param key        the key to get the String setting for
	 * @param defaultVal the default value to return if the setting was not specified
	 * @return the String value for the setting
	 */
	public String getStringSetting(String key, String defaultVal) {
		final Object value = this.getSettings().get(key);
		return value != null ? value.toString() : defaultVal;
	}

	/**
	 * Gets an int value for the setting, returning the default value if not
	 * specified.
	 *
	 * @param key        the key to get the int value for
	 * @param defaultVal default value if the setting was not specified
	 * @return the int value for the setting
	 * @throws IllegalArgumentException if setting does not contain an int
	 */
	protected int getIntSetting(String key, int defaultVal) throws IllegalArgumentException {
		if (settings.containsKey(key)) {
			final String value = settings.get(key).toString();
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				throw new IllegalArgumentException("Setting '" + key + "=" + value + "' is not an integer on " + this.toString());
			}
		} else {
			return defaultVal;
		}
	}

	/** */
	@JsonIgnore
	public boolean isDebugEnabled() {
		return debugEnabled != null ? debugEnabled : getBooleanSetting(DEBUG);
	}

	/** */
	@JsonIgnore
	@SuppressWarnings("unchecked")
	public List<String> getTypeNames() {
		if (!this.getSettings().containsKey(TYPE_NAMES)) {
			List<String> tmp = new ArrayList<String>();
			this.getSettings().put(TYPE_NAMES, tmp);
		}
		return (List<String>) this.getSettings().get(TYPE_NAMES);
	}

	/** */
	public void setTypeNames(List<String> typeNames) {
		this.getSettings().put(TYPE_NAMES, typeNames);
	}

	/** */
	public void addTypeName(String str) {
		this.getTypeNames().add(str);
	}

	/**
	 * Given a typeName string, get the first match from the typeNames setting.
	 * In other words, suppose you have:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the
	 * string
	 */
	protected String getConcatedTypeNameValues(String typeNameStr) {
		return KeyUtils.getConcatedTypeNameValues(this.getTypeNames(), typeNameStr);
	}

	/**
	 * Replaces all . with _ and removes all spaces and double/single quotes.
	 */
	protected String cleanupStr(String name) {
		return StringUtils.cleanupStr(name);
	}

	/**
	 * A do nothing method.
	 */
	@Override
	public void start() throws LifecycleException {
		// Do nothing.
	}

	/**
	 * A do nothing method.
	 */
	@Override
	public void stop() throws LifecycleException {
		// Do nothing.
	}
}
