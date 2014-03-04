package com.googlecode.jmxtrans.model.output;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import java.io.IOException;
import org.slf4j.Logger;

/**
 * Extension of KeyOutWriter to use Logback for its logging.  This version supports outputting with a time and size
 * based rollover policy but with no file rename taking place.  The filename remains constant for the output file from
 * its creation to the roll over to a new file.  Then the old file remains with its old name and the new file is used
 * for further logging.
 * 
 * We found this to be useful when dealing with forwarding logs on Windows as our other support applications were not
 * able to handle the rename correctly on Windows and DailyKeyOutWriter caused the original file to be renamed.  
 * Also DailyRollingAppender has bugs that can cause data loss and it is no longer recommended for use judging by the
 * javadocs for it.
 * 
 * The basic operation is to set the date time to rollover in the outputFile setting.  For example:
 *      "outputFile": "testing-here.log%d{yyyy-MM-dd}.%i",
 * In this case we have set the logging to roll over every day.  There is also a default max size as per the
 * KeyOutWriter.  If the file exceeds the default max the .%i integer is increments.  See the Logstash documentation
 * for more information.
 */
public class TimeBasedRollingKeyOutWriter extends KeyOutWriter {
    
    private static final LoggerContext loggerContext = new LoggerContext();
    private static final String DEFAULT_OUTPUT_PATTERN = "%msg%n";
    private static final String SETTING_OUTPUT_PATTERN = "outputPattern";

    @Override
    protected Logger initLogger(String fileStr) throws IOException {
        RollingPolicy rollingPolicy = initRollingPolicy(fileStr, getSettingMaxFileHistory(), getSettingMaxFileSize());
        RollingFileAppender appender = buildAppender(buildEncoder(), rollingPolicy);
        
        rollingPolicy.start();
        appender.start();
        
        // configure the logger for info and add the appender
        return getAndConfigureLogger(appender);
   }
    
    private RollingFileAppender buildAppender(Encoder encoder, RollingPolicy rollingPolicy) {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setEncoder(encoder);
        appender.setAppend(true);
        appender.setContext(loggerContext);
        appender.setRollingPolicy(rollingPolicy);
        
        rollingPolicy.setParent(appender);
        return appender;
    }
    
    protected Logger getAndConfigureLogger(Appender appender) {
        ch.qos.logback.classic.Logger loggerLocal = loggerContext.getLogger(buildLoggerName());
        loggerLocal.setAdditive(false);
        loggerLocal.setLevel(Level.INFO);
        loggerLocal.addAppender(appender);
        return loggerLocal;
    }
    
    protected Encoder buildEncoder() {
        PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(loggerContext);
        logEncoder.setPattern(getSettingOutputPattern());
        logEncoder.start();
        
        return logEncoder;
    }
                
    protected RollingPolicy initRollingPolicy(String fileName, int maxBackupFiles, String maxFileSize) {
        SizeAndTimeBasedFNATP sizeTimeBasedPolicy = new SizeAndTimeBasedFNATP();
        // the max file size before rolling to a new file
        sizeTimeBasedPolicy.setMaxFileSize(maxFileSize);
        sizeTimeBasedPolicy.setContext(loggerContext);
        
        TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
        // set the filename pattern
        policy.setFileNamePattern(fileName);
        // the maximum number of backup files to keep around
        policy.setMaxHistory(maxBackupFiles);
        policy.setTimeBasedFileNamingAndTriggeringPolicy(sizeTimeBasedPolicy);
        policy.setContext(loggerContext);
        
        return policy;
    }
    
    protected String getSettingOutputPattern() {
        String outputPattern = (String) this.getSettings().get(SETTING_OUTPUT_PATTERN);
        if (outputPattern == null) {
            return DEFAULT_OUTPUT_PATTERN;
        }
        else {
            return outputPattern;
        }
    }
}
