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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.PatternLayout;

import java.io.IOException;
import java.util.Map;

/**
 * This class is derived from KeyOutWriter. It uses DailyRollingFileAppender
 * instead of RollingFileAppender.
 * 
 * Writes out data in the same format as the GraphiteWriter, except to a file
 * and tab delimited. Takes advantage of Log4J DailyRollingFileAppender to
 * automatically handle rolling the files at defined intervals (datePattern).
 * 
 * Note that this writer will NOT clean up after itself. The files will not be
 * deleted. It is up to the user to clean things up.
 * 
 * The datePattern is taken directly from DailyRollingFileAppender DatePattern.
 * 
 * The default datePattern will roll the files at midnight once a day
 * ("'.'yyyy-MM-dd") See the documentation for DailyRollingFileAppender for
 * other useful patterns.
 * 
 */
public class DailyKeyOutWriter extends KeyOutWriter {

	private static final String DATE_PATTERN = "'.'yyyy-MM-dd";

	private final String datePattern;

	@JsonCreator
	public DailyKeyOutWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("maxLogFileSize") String maxLogFileSize,
			@JsonProperty("maxLogBackupFiles") int maxLogBackupFiles,
			@JsonProperty("delimiter") String delimiter,
			@JsonProperty("datePattern") String datePattern,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, outputFile, maxLogFileSize, maxLogBackupFiles, delimiter, settings);
		this.datePattern = firstNonNull(
				datePattern,
				(String) getSettings().get("datePattern"),
				DATE_PATTERN
		);
	}

	/**
	 * The maxLogFileSize and maxLogBackupFiles are ignored as per the existing behaviour of DailyKeyOutWriter.
	 */
	@Override
	protected Appender buildLog4jAppender(String fileStr, String maxLogFileSize, Integer maxLogBackupFiles)
			throws IOException {
		return new DailyRollingFileAppender(new PatternLayout(LOG_PATTERN), fileStr, datePattern);
	}
	
	@Override
	protected String buildLoggerName() {
		return "DailyKeyOutWriter" + this.hashCode();
	}

	public String getDatePattern() {
		return datePattern;
	}
}
