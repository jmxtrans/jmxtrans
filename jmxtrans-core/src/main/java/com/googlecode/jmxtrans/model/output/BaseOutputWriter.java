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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.typename.TypeNameValuesStringBuilder;
import com.googlecode.jmxtrans.model.results.BooleanAsNumberValueTransformer;
import com.googlecode.jmxtrans.model.results.IdentityValueTransformer;
import com.googlecode.jmxtrans.model.results.ResultValuesTransformer;
import com.googlecode.jmxtrans.model.results.ValueTransformer;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.googlecode.jmxtrans.model.output.Settings.getBooleanSetting;

/**
 * Implements the common code for output filters.
 *
 * Note that the use of a non threadsafe @link{Map} makes this class non threadsafe.
 *
 * @author jon
 */
@NotThreadSafe
@ToString
public abstract class BaseOutputWriter implements OutputWriter, OutputWriterFactory {

	private static final Logger logger = LoggerFactory.getLogger(BaseOutputWriter.class);

	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String TEMPLATE_FILE = "templateFile";
	public static final String BINARY_PATH = "binaryPath";
	public static final String DEBUG = "debug";
	public static final String TYPE_NAMES = "typeNames";
	public static final String BOOLEAN_AS_NUMBER = "booleanAsNumber";

	@Getter private ImmutableList<String> typeNames;
	@Getter	private boolean debugEnabled;
	private Map<String, Object> settings;
	private final ValueTransformer valueTransformer;

	@JsonCreator
	public BaseOutputWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {

		if (settings != null && !settings.isEmpty()) {
			logger.warn("Using 'settings' is deprecated, please pass attributes directly to the OutputWriter.");
		}

		// resolve and initialize settings first, so we can refer to them to initialize other fields
		this.settings = copyOf(MoreObjects.firstNonNull(
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

		// Get the value of the boolean from the JSON settings if it exists, otherwise default it to the value
		// of the boolean passed into the Constructor.
		booleanAsNumber = getBooleanSetting(this.settings, BOOLEAN_AS_NUMBER, booleanAsNumber);

		if (booleanAsNumber) {
			this.valueTransformer = new BooleanAsNumberValueTransformer(1, 0);
		} else {
			this.valueTransformer = new IdentityValueTransformer();
		}
	}

	protected <T> T firstNonNull(@Nullable T first, @Nullable T second, @Nullable T third) {
		return first != null ? first : (second != null ? second : checkNotNull(third));
	}
	
	/**
	 * @deprecated Don't use the settings Map, please extract necessary bits at construction time.
	 */
	@Deprecated @Override
	public Map<String, Object> getSettings() {
		return settings;
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
		return TypeNameValuesStringBuilder.getDefaultBuilder().build(this.getTypeNames(), typeNameStr);
	}

	@Override
	public void start() throws LifecycleException {
		// Do nothing.
	}

	@Override
	public void close() throws LifecycleException {
		// Do nothing.
	}

	@Override
	public final void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
		internalWrite(server, query, from(results).transform(new ResultValuesTransformer(valueTransformer)).toList());
	}

	protected abstract void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception;

	@Override
	public OutputWriter create() {
		return this;
	}

}
