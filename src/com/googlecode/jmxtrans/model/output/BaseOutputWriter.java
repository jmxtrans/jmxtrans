package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.naming.KeyUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveMap;
import static com.googlecode.jmxtrans.model.output.Settings.getBooleanSetting;

/**
 * Implements the common code for output filters.
 *
 * Note that the use of a non threadsafe @link{Map} makes this class non threadsafe.
 *
 * @author jon
 */
@NotThreadSafe
public abstract class BaseOutputWriter implements OutputWriter {

	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String TEMPLATE_FILE = "templateFile";
	public static final String BINARY_PATH = "binaryPath";
	public static final String DEBUG = "debug";
	public static final String TYPE_NAMES = "typeNames";

	private ImmutableList<String> typeNames;
	private boolean debugEnabled;
	private Map<String, Object> settings;

	@JsonCreator
	public BaseOutputWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {
		// resolve and initialize settings first, so we cean refer to them to initialize other fields
		this.settings = resolveMap(MoreObjects.firstNonNull(
				settings,
				Collections.<String, Object>emptyMap()));

		this.typeNames = copyOf(firstNonNull(
				typeNames,
				(List<String>) this.settings.get(TYPE_NAMES),
				Collections.<String>emptyList()));
		this.debugEnabled = firstNonNull(
				debugEnabled,
				getBooleanSetting(this.settings, DEBUG),
				false);
	}

	protected <T> T firstNonNull(@Nullable T first, @Nullable T second, @Nullable T third) {
		return first != null ? first : (second != null ? second : checkNotNull(third));
	}

	/**
	 * @deprecated Don't use the settings Map, please extract necessary bits at construction time.
	 */
	@Deprecated
	public Map<String, Object> getSettings() {
		return settings;
	}

	/**
	 * @deprecated Initialize settings in constructor only please.
	 */
	@Deprecated
	public void setSettings(Map<String, Object> settings) {
		this.settings = resolveMap(settings);
		if (settings.containsKey(DEBUG)) {
			this.debugEnabled = getBooleanSetting(settings, DEBUG);
		}
		if (settings.containsKey(TYPE_NAMES)) {
			this.typeNames = copyOf((List<String>) settings.get(TYPE_NAMES));
		}
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public List<String> getTypeNames() {
		return typeNames;
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
