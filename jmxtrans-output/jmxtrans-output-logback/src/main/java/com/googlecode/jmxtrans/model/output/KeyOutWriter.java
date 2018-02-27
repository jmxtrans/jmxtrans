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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

/**
 * Writes out data in the same format as the GraphiteWriter, except to a file
 * and tab delimited. Takes advantage of Logback RollingFileAppender to
 * automatically handle rolling the files after they reach a certain size.
 * <p/>
 * The default max size of the log files are 10MB (maxLogFileSize) The default
 * number of rolled files to keep is 200 (maxLogBackupFiles)
 *
 * @author jon
 */
public class KeyOutWriter extends BaseOutputWriter {

	/**
	 * Logback logger factory
	 */
	protected static final LoggerContext loggerContext = new LoggerContext();
	protected static final String SETTING_MAX_LOG_FILE_SIZE = "maxLogFileSize";
	protected static final String SETTING_MAX_BACK_FILES = "maxLogBackupFiles";
	protected static final String SETTING_LOG_PATTERN = "logPattern";
	protected static final String SETTING_DELIMITER = "delimiter";
	protected static final int LOG_IO_BUFFER_SIZE_BYTES = 1024;
	protected static final Map<String, Logger> loggers = new ConcurrentHashMap<>();

	protected static final int MAX_LOG_BACKUP_FILES = 200;
	protected static final String MAX_LOG_FILE_SIZE = "10MB";
	protected static final String DEFAULT_LOG_PATTERN = "%msg%n";
	protected static final String DEFAULT_DELIMITER = "\t";
	protected Logger logger;


	private final String outputFile;
	private final String maxLogFileSize;
	private final int maxLogBackupFiles;
	private final String delimiter;
	private final String logPattern;

	@JsonCreator
	public KeyOutWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("maxLogFileSize") String maxLogFileSize,
			@JsonProperty("maxLogBackupFiles") Integer maxLogBackupFiles,
			@JsonProperty("logPattern") String logPattern,
			@JsonProperty("delimiter") String delimiter,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.outputFile = MoreObjects.firstNonNull(
				outputFile,
				(String) getSettings().get("outputFile"));
		this.maxLogFileSize = firstNonNull(
				maxLogFileSize,
				(String) getSettings().get(SETTING_MAX_LOG_FILE_SIZE),
				MAX_LOG_FILE_SIZE);
		this.maxLogBackupFiles = firstNonNull(
				maxLogBackupFiles,
				(Integer) getSettings().get(SETTING_MAX_BACK_FILES),
				MAX_LOG_BACKUP_FILES);
		this.logPattern = firstNonNull(
				logPattern,
				(String) getSettings().get(SETTING_LOG_PATTERN),
				DEFAULT_LOG_PATTERN
		);
		this.delimiter = firstNonNull(
				delimiter,
				(String) getSettings().get(SETTING_DELIMITER),
				DEFAULT_DELIMITER
		);
	}

	/**
	 * Creates the logging
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		// Check if we've already created a logger for this file. If so, use it.
		if (loggers.containsKey(outputFile)) {
			logger = getLogger(outputFile);
			return;
		}
		// need to create a logger
		try {
			logger = buildLogger(outputFile);
			loggers.put(outputFile, logger);
		} catch (IOException e) {
			throw new ValidationException("Failed to setup logback", query, e);
		}
	}

	@VisibleForTesting
	Logger getLogger(String outputFile) {
		return loggers.get(outputFile);
	}

	/**
	 * The meat of the output. Very similar to GraphiteWriter.
	 */
	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		List<String> typeNames = getTypeNames();

		for (Result result : results) {
			if (isNumeric(result.getValue())) {

				logger.info(KeyUtils.getKeyString(server, query, result, typeNames, null) + delimiter
						+ result.getValue().toString() + delimiter + result.getEpoch());
			}
		}
	}

	/**
	 * Initializes the logger. This is called when we need to create a new
	 * logger for the given file name.
	 *
	 * @param fileStr
	 * @return a new Logger instance for the given fileStr
	 * @throws IOException
	 */
	protected Logger buildLogger(String fileStr) throws IOException {
		String loggerName = buildLoggerName();
		Appender appender = buildAppender(loggerName, fileStr);

		// Create the logger and add to the map of loggers using our factory
		Logger logger = loggerContext.getLogger(loggerName);
		logger.addAppender(appender);
		logger.setLevel(Level.INFO);
		logger.setAdditive(false);
		return logger;
	}

	protected String buildLoggerName() {
		return "KeyOutWriter" + this.hashCode();
	}

	protected Encoder buildEncoder() {
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern(logPattern);
		encoder.start();
		return encoder;
	}

	protected RollingPolicy buildRollingPolicy(FileAppender<?> appender, String fileStr) {
		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setParent(appender);
		rollingPolicy.setContext(loggerContext);
		rollingPolicy.setMinIndex(1);
		rollingPolicy.setMaxIndex(maxLogBackupFiles);
		rollingPolicy.setFileNamePattern(fileStr + ".%i");
		rollingPolicy.start();
		return rollingPolicy;
	}

	protected TriggeringPolicy buildTriggeringPolicy() {
		SizeBasedTriggeringPolicy triggeringPolicy = new SizeBasedTriggeringPolicy();
		triggeringPolicy.setContext(loggerContext);
		triggeringPolicy.setMaxFileSize(FileSize.valueOf(maxLogFileSize));
		triggeringPolicy.start();
		return triggeringPolicy;
	}

	protected Appender buildAppender(String loggerName, String fileStr) {
		final RollingFileAppender appender = new RollingFileAppender();
		appender.setName(loggerName + "RollingFile");
		appender.setContext(loggerContext);
		appender.setImmediateFlush(true);
		appender.setBufferSize(new FileSize(LOG_IO_BUFFER_SIZE_BYTES));
		appender.setFile(fileStr);
		appender.setEncoder(buildEncoder());
		TriggeringPolicy triggeringPolicy = buildTriggeringPolicy();
		if (triggeringPolicy != null) {
			appender.setTriggeringPolicy(triggeringPolicy);
		}
		appender.setRollingPolicy(buildRollingPolicy(appender, fileStr));
		appender.start();
		return appender;
	}

	@Override
	public void close() {
		for(Logger logger: this.loggers.values()) {
			logger.detachAndStopAllAppenders();
		}
		this.loggers.clear();
	}


	public String  getMaxLogFileSize() {
		return maxLogFileSize;
	}

	public Integer getMaxLogBackupFiles() {
		return maxLogBackupFiles;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public String getOutputFile() {
		return outputFile;
	}
}
