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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.impl.Log4jLoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes out data in the same format as the GraphiteWriter, except to a file
 * and tab delimited. Takes advantage of Log4J RollingFileAppender to
 * automatically handle rolling the files after they reach a certain size.
 * <p/>
 * The default max size of the log files are 10MB (maxLogFileSize) The default
 * number of rolled files to keep is 200 (maxLogBackupFiles)
 *
 * @author jon
 */
public class KeyOutWriter extends BaseOutputWriter {

	protected static final Log4jLoggerFactory log4jLoggerFactory = new Log4jLoggerFactory();
	protected static final String SETTING_MAX_LOG_FILE_SIZE = "maxLogFileSize";
	protected static final String SETTING_MAX_BACK_FILES = "maxLogBackupFiles";
	protected static final String SETTING_DELIMITER = "delimiter";
	protected static final String LOG_PATTERN = "%m%n";
	protected static final int LOG_IO_BUFFER_SIZE_BYTES = 1024;
	protected static final Map<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

	protected static final int MAX_LOG_BACKUP_FILES = 200;
	protected static final String MAX_LOG_FILE_SIZE = "10MB";
	protected static final String DEFAULT_DELIMITER = "\t";
	protected Logger logger;


	private final String outputFile;
	private final String maxLogFileSize;
	private final int maxLogBackupFiles;
	private final String delimiter;

	@JsonCreator
	public KeyOutWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("maxLogFileSize") String maxLogFileSize,
			@JsonProperty("maxLogBackupFiles") int maxLogBackupFiles,
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
			logger = loggers.get(outputFile);
			return;
		}
		// need to create a logger
		try {
			logger = initLogger(outputFile);
			loggers.put(outputFile, logger);
		} catch (IOException e) {
			throw new ValidationException("Failed to setup log4j", query);
		}
	}

	/**
	 * The meat of the output. Very similar to GraphiteWriter.
	 */
	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		List<String> typeNames = getTypeNames();

		for (Result result : results) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {

						logger.info(KeyUtils.getKeyString(server, query, result, values, typeNames, null) + delimiter
								+ values.getValue().toString() + delimiter + result.getEpoch());
					}
				}
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
	protected Logger initLogger(String fileStr) throws IOException {
		Appender appender = buildLog4jAppender(fileStr, getMaxLogFileSize(), getMaxLogBackupFiles());
		LoggerFactory loggerFactory = buildLog4jLoggerFactory(appender);

		String loggerKey = buildLoggerName();

		// Create the logger and add to the map of loggers using our factory
		LogManager.getLogger(loggerKey, loggerFactory);
		return log4jLoggerFactory.getLogger(loggerKey);
	}

	protected String buildLoggerName() {
		return "KeyOutWriter" + this.hashCode();
	}

	protected Appender buildLog4jAppender(
			String fileStr, String maxLogFileSize,
			Integer maxLogBackupFiles) throws IOException {

		PatternLayout pl = new PatternLayout(LOG_PATTERN);
		final RollingFileAppender appender = new RollingFileAppender(pl, fileStr, true);
		appender.setImmediateFlush(true);
		appender.setBufferedIO(false);
		appender.setBufferSize(LOG_IO_BUFFER_SIZE_BYTES);
		appender.setMaxFileSize(maxLogFileSize);
		appender.setMaxBackupIndex(maxLogBackupFiles);

		return appender;
	}

	protected LoggerFactory buildLog4jLoggerFactory(final Appender appender) {
		return new LoggerFactory() {
			@Override
			public org.apache.log4j.Logger makeNewLoggerInstance(String name) {
				org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(name);
				logger.addAppender(appender);
				logger.setLevel(org.apache.log4j.Level.INFO);
				logger.setAdditivity(false);
				return logger;
			}
		};
	}

	public String getMaxLogFileSize() {
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
