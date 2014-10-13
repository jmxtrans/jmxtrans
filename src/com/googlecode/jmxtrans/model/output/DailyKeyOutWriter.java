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
