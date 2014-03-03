package com.googlecode.jmxtrans.model.output;

import static com.googlecode.jmxtrans.model.output.KeyOutWriter.LOG_PATTERN;
import java.io.IOException;
import org.apache.log4j.Appender;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.PatternLayout;

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
    
    /**
     * The maxLogFileSize and maxLogBackupFiles are ignored as per the existing behaviour of DailyKeyOutWriter.
     */
    @Override
    protected Appender buildLog4jAppender(String fileStr, String maxLogFileSize, Integer maxLogBackupFiles)
            throws IOException {
        String datePattern = (String) this.getSettings().get("datePattern");
		if (datePattern == null) {
			datePattern = DATE_PATTERN;
		}
		PatternLayout pl = new PatternLayout(LOG_PATTERN);
        DailyRollingFileAppender appender = new DailyRollingFileAppender(pl, fileStr, datePattern);
        
        return appender;
    }
    
    @Override
    protected String buildLoggerName() {
        return "DailyKeyOutWriter" + this.hashCode();
    }
    
}
