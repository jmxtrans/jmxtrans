package com.googlecode.jmxtrans.model.output;

import java.io.IOException;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggerFactory;

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

	public DailyKeyOutWriter() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.googlecode.jmxtrans.model.output.KeyOutWriter#initLogger(java.lang
	 * .String)
	 */
	@Override
	protected Logger initLogger(String fileStr) throws IOException {
		String datePattern = (String) this.getSettings().get("datePattern");
		if (datePattern == null) {
			datePattern = DATE_PATTERN;
		}
		PatternLayout pl = new PatternLayout(LOG_PATTERN);

		final DailyRollingFileAppender appender = new DailyRollingFileAppender(pl, fileStr, datePattern);
		appender.setImmediateFlush(true);
		appender.setBufferedIO(false);
		appender.setBufferSize(LOG_IO_BUFFER_SIZE_BYTES);

		LoggerFactory loggerFactory = new LoggerFactory() {
			@Override
			public Logger makeNewLoggerInstance(String name) {
				Logger logger = Logger.getLogger(name);
				logger.addAppender(appender);
				logger.setLevel(Level.INFO);
				logger.setAdditivity(false);
				return logger;
			}
		};
		return loggerFactory.makeNewLoggerInstance("DailyKeyOutWriter" + this.hashCode());
	}
}
