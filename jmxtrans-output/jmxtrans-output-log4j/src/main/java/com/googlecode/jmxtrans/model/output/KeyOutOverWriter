package com.googlecode.jmxtrans.model.output;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * Writes out data and overwrites sample data
 * 
 *
 * @author Surjit Bains <surjit.bains@polarpoint.co.uk>
 */



public class KeyOutOverWriter extends KeyOutWriter {

	
	@JsonCreator
	public KeyOutOverWriter(
			
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("maxLogFileSize") String maxLogFileSize,
			@JsonProperty("maxLogBackupFiles") int maxLogBackupFiles,
			@JsonProperty("delimiter") String delimiter,
			@JsonProperty("datePattern") String datePattern,
			@JsonProperty("settings") Map<String, Object> settings) {
		
		super(typeNames, booleanAsNumber, debugEnabled, outputFile, maxLogFileSize, maxLogBackupFiles, delimiter,
				settings);
	
	}
	
	/**
	 * The maxLogFileSize and maxLogBackupFiles are ignored as per the existing behavior of DailyKeyOutWriter.
	 * 
	 */
	@Override
	protected Appender buildLog4jAppender(String fileStr, String maxLogFileSize, Integer maxLogBackupFiles)
			throws IOException {
		return new FileAppender(new PatternLayout(LOG_PATTERN), fileStr, false); // overwrite file 
	}
	
	@Override
	protected String buildLoggerName() {
		return "KeyOutOverWriter" + this.hashCode();
	}

}
