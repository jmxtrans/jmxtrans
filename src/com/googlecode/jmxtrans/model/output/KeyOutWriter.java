package com.googlecode.jmxtrans.model.output;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerFactory;
import org.slf4j.impl.Log4jLoggerFactory;

/**
 * Writes out data in the same format as the GraphiteWriter, except to a file
 * and tab delimited. Takes advantage of Log4J RollingFileAppender to
 * automatically handle rolling the files after they reach a certain size.
 * 
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
    protected String delimiter = DEFAULT_DELIMITER;

	public KeyOutWriter() {
	}

	/**
	 * Creates the logging
	 */
	@Override
	public void validateSetup(Query query) throws ValidationException {
		String fileStr = (String) this.getSettings().get("outputFile");
        delimiter = getDelimiter();
		if (fileStr == null) {
			throw new ValidationException("You must specify an outputFile setting.", query);
		}
		// Check if we've already created a logger for this file. If so, use it.
		if (loggers.containsKey(fileStr)) {
			logger = loggers.get(fileStr);
			return;
		}
		// need to create a logger
		try {
			logger = initLogger(fileStr);
			loggers.put(fileStr, logger);
		} catch (IOException e) {
			throw new ValidationException("Failed to setup log4j", query);
		}
	}

	/**
	 * The meat of the output. Very similar to GraphiteWriter.
	 */
	@Override
	public void doWrite(Query query) throws Exception {
		List<String> typeNames = getTypeNames();

		for (Result result : query.getResults()) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Entry<String, Object> values : resultValues.entrySet()) {
					if (JmxUtils.isNumeric(values.getValue())) {
						StringBuilder sb = new StringBuilder();

						sb.append(JmxUtils.getKeyString(query, result, values, typeNames, null));
						sb.append(delimiter);
						sb.append(values.getValue().toString());
						sb.append(delimiter);
						sb.append(result.getEpoch());

						logger.info(sb.toString());
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
		String maxLogFileSize = getSettingMaxFileSize();
		Integer maxLogBackupFiles = getSettingMaxFileHistory();
        
        Appender appender = buildLog4jAppender(fileStr, maxLogFileSize, maxLogBackupFiles);

		LoggerFactory loggerFactory = buildLog4jLoggerFactory(appender);
        
        String loggerKey = buildLoggerName();
        
        // Create the logger and add to the map of loggers using our factory
        LogManager.getLogger(loggerKey, loggerFactory);
        Logger loggerToReturn = log4jLoggerFactory.getLogger(loggerKey);
        
        return loggerToReturn;
	}
    
    protected String buildLoggerName() {
        return "KeyOutWriter" + this.hashCode();
    }
    
    protected Appender buildLog4jAppender(String fileStr, String maxLogFileSize,
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
        LoggerFactory loggerFactory = new LoggerFactory() {
            @Override
			public org.apache.log4j.Logger makeNewLoggerInstance(String name) {
				org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(name);
				logger.addAppender(appender);
				logger.setLevel(org.apache.log4j.Level.INFO);
				logger.setAdditivity(false);
				return logger;
			}
		};
            
        return loggerFactory;
    }
    
    protected String getSettingMaxFileSize() {
        String maxLogFileSize = (String) this.getSettings().get(SETTING_MAX_LOG_FILE_SIZE);
        if (maxLogFileSize == null) {
            return MAX_LOG_FILE_SIZE;
        }
        else {
            return maxLogFileSize;
        }
    }
    
    protected Integer getSettingMaxFileHistory() {
        Integer maxLogBackupFiles = (Integer) this.getSettings().get(SETTING_MAX_BACK_FILES);
        if (maxLogBackupFiles == null) {
            return MAX_LOG_BACKUP_FILES;
        }
        else {
            return maxLogBackupFiles;
        }
    }
    
    protected String getDelimiter() {
        String delimiterLocal = (String) this.getSettings().get(SETTING_DELIMITER);
        if (delimiterLocal == null) {
            return DEFAULT_DELIMITER;
        }
        else {
            return delimiterLocal;
        }
    }

}
