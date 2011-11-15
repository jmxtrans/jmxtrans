package com.googlecode.jmxtrans.model.output;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * Writes out data in the same format as the GraphiteWriter, except to a file and tab delimited.
 * Takes advantage of Log4J RollingFileAppender to automatically handle rolling the files after
 * they reach a certain size.
 *
 * The default max size of the log files are 10MB (maxLogFileSize)
 * The default number of rolled files to keep is 200 (maxLogBackupFiles)
 *
 * @author jon
 */
public class KeyOutWriter extends BaseOutputWriter {

	private static final String LOG_PATTERN = "%m%n";
	private static final int LOG_IO_BUFFER_SIZE_BYTES = 1024;
	private static final int MAX_LOG_BACKUP_FILES = 200;
	private static final String MAX_LOG_FILE_SIZE = "10MB";
	private Logger logger;

	public KeyOutWriter() {
	}

	/**
	 * Creates the logging
	 */
	public void validateSetup(Query query) throws ValidationException {
		String fileStr = (String)this.getSettings().get("outputFile");
		if (fileStr == null) {
			throw new ValidationException("You must specify an outputFile setting.", query);
		}

		try {
			String maxLogFileSize = (String)this.getSettings().get("maxLogFileSize");
			Integer maxLogBackupFiles = (Integer)this.getSettings().get("maxLogBackupFiles");
			PatternLayout pl = new PatternLayout(LOG_PATTERN);

			final RollingFileAppender appender = new RollingFileAppender(pl, fileStr, true);
			appender.setImmediateFlush(true);
			appender.setBufferedIO(false);
			appender.setBufferSize(LOG_IO_BUFFER_SIZE_BYTES);

			if (maxLogFileSize == null) {
				appender.setMaxFileSize(MAX_LOG_FILE_SIZE);
			} else {
				appender.setMaxFileSize(maxLogFileSize);
			}

			if (maxLogBackupFiles == null) {
				appender.setMaxBackupIndex(MAX_LOG_BACKUP_FILES);
			} else {
				appender.setMaxBackupIndex(maxLogBackupFiles);
			}

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

			logger = loggerFactory.makeNewLoggerInstance("KeyOutWriter" + this.hashCode());
		} catch (IOException e) {
			throw new ValidationException("Failed to setup log4j", query);
		}
	}

	/**
	 * The meat of the output. Very similar to GraphiteWriter.
	 */
	public void doWrite(Query query) throws Exception {
		List<String> typeNames = getTypeNames();

		for (Result result : query.getResults()) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Entry<String, Object> values : resultValues.entrySet()) {
					if (JmxUtils.isNumeric(values.getValue())) {
						StringBuilder sb = new StringBuilder();

						sb.append(JmxUtils.getKeyString(query, result, values, typeNames, null));
						sb.append("\t");
						sb.append(values.getValue());
						sb.append("\t");
						sb.append(result.getEpoch());

						logger.info(sb.toString());
					}
				}
			}
		}
	}
}
