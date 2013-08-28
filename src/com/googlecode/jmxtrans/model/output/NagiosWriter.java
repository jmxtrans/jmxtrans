package com.googlecode.jmxtrans.model.output;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.spi.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;


/**
 * Writes out data in Nagios format to a file, it should be used with Nagios external command file.
 * 
 * @author Denis "Thuck" Doria <denisdoria@gmail.com>
 */
public class NagiosWriter extends BaseOutputWriter {

    protected static final String LOG_PATTERN = "%m%n";
    protected static final int LOG_IO_BUFFER_SIZE_BYTES = 1024;
    protected static final Map<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    private static final String NAGIOS_HOST = "nagiosHost";
    private static final String PREFIX = "prefix";
    private static final String POSFIX = "posfix";
    private static final String FILTERS = "filters";
    private static final String THRESHOLDS = "thresholds";

    protected Logger logger;

    public NagiosWriter() {
    }

    /**
     * Initial log setup.
     */
    @Override
    public void validateSetup(Query query) throws ValidationException {
        List<String> filters = (List<String>) this.getSettings().get(FILTERS);
        List<String> thresholds = (List<String>) this.getSettings().get(THRESHOLDS);
        String hostNagios = (String) this.getSettings().get(NAGIOS_HOST);

        if (filters.size() != thresholds.size()){
             throw new ValidationException("filters and thresholds must have the same size.", query);

        }
        if (hostNagios == null){
            throw new ValidationException("nagiosHost cannot be empty", query);

        }

        checkFile(query);
    }

    /**
     * Creates the logging. Nagios doesn't start if the external command pipe
     * exists, so we write to /dev/null until we have it available.
     * From the official documentation: 
     *   The external command file is implemented as a named pipe (FIFO), 
     *   which is created when Nagios starts and removed when it shuts down.
     *   If the file exists when Nagios starts, 
     *   the Nagios process will terminate with an error message. 
     *http://nagios.sourceforge.net/docs/3_0/configmain.html#command_file
     */
    public void checkFile(Query query) throws ValidationException{
        String fileStr = (String) this.getSettings().get("outputFile");
        if (fileStr == null) {
                throw new ValidationException("You must specify an outputFile setting.", query);
            }
    
        File nagiosPipe = new File(fileStr);
        if (!nagiosPipe.exists()) {
            if (loggers.containsKey("/dev/null")){
                logger = loggers.get("/dev/null");

            }
            else {
              try {
               logger = initLogger("/dev/null");
               loggers.put("/dev/null", logger);

              } catch (IOException e) {
               throw new ValidationException("Failed to setup log4j", query);

              }

            }

            if (loggers.containsKey(fileStr)){
                loggers.remove(fileStr);
            }
            return;
        }
        else if (loggers.containsKey(fileStr)){
                logger = loggers.get(fileStr);
                return;
        }
    
        try {
            logger = initLogger(fileStr);
            loggers.put(fileStr, logger);
    
        } catch (IOException e) {
            throw new ValidationException("Failed to setup log4j", query);
    
        }
    }


    /**
     * The meat of the output. Nagios format..
     */
    @Override
    public void doWrite(Query query) throws Exception {
        checkFile(query);
        List<String> typeNames = getTypeNames();
        String hostNagios = (String) this.getSettings().get(NAGIOS_HOST);
        String prefix = (String) this.getSettings().get(PREFIX);
        String posfix = (String) this.getSettings().get(POSFIX);

        List<String> filters = (List<String>) this.getSettings().get(FILTERS);
        List<String> thresholds = (List<String>) this.getSettings().get(THRESHOLDS);

        for (Result result : query.getResults()) {
            Map<String, Object> resultValues = result.getValues();
            if (resultValues != null) {
                for (Entry<String, Object> values : resultValues.entrySet()) {
                    String[] str_array = JmxUtils.getKeyString(query, result, values, typeNames, null).split("\\.");
                    if (JmxUtils.isNumeric(values.getValue()) && filters.contains(str_array[2])) {
                        int threshold_pos = filters.indexOf(str_array[2]);
                        StringBuilder sb = new StringBuilder();

                        sb.append("[");
                        sb.append(result.getEpoch());
                        sb.append("] PROCESS_SERVICE_CHECK_RESULT;");
                        sb.append(hostNagios);
                        sb.append(";");
                        if (prefix != null){
                            sb.append(prefix);
                        }
                        sb.append(str_array[2]);
                        if (posfix != null){
                            sb.append(posfix);
                        }
                        sb.append(";");
                        sb.append(nagiosCheckValue(values.getValue().toString(), thresholds.get(threshold_pos)));
                        sb.append(";");
                        //Missing the performance information

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
        PatternLayout pl = new PatternLayout(LOG_PATTERN);

        final FileAppender appender = new FileAppender(pl, fileStr, true);
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
        return loggerFactory.makeNewLoggerInstance("NagiosWriter" + this.hashCode());
    }

    /**
     * Define if a value is in a critical, warning or ok state.
     */
    protected String nagiosCheckValue(String value, String composeRange){
        List<String> simpleRange = new ArrayList<String>(Arrays.asList(composeRange.split(",")));
        double value_d = Double.parseDouble(value);

        if (composeRange.isEmpty()){
            return "0";
        }

        if (simpleRange.size() == 1){
            if (composeRange.endsWith(",")){
                if (valueCheck(value_d, simpleRange.get(0))){
                    return "1";
                }
                else {return "0";}
            }
            else if (valueCheck(value_d, simpleRange.get(0))){
                    return "2";

            }
            else {return "0";}
        }

        if (valueCheck(value_d, simpleRange.get(1))){
            return "2";
        }

        if (valueCheck(value_d, simpleRange.get(0))){
            return "1";

        }

        return "0";

    }

    /**
     * Check if a value is inside of a range defined in the thresholds.
     * This check is based on Nagios range definition.
     * http://nagiosplug.sourceforge.net/developer-guidelines.html#THRESHOLDFORMAT
     */
    protected boolean valueCheck(double value, String simpleRange){
        if (simpleRange.isEmpty()){
            return false;
        }

        if (simpleRange.endsWith(":")) {
            if (value < Double.parseDouble(simpleRange.replace(":",""))){
                return true;
            }
            else {return false;}

        }

        if (simpleRange.startsWith("~:")){
            if (value > Double.parseDouble(simpleRange.replace("~:",""))){
                return true;
            }else {return false;}

        }

        if (simpleRange.startsWith("@")){
            String[] values = simpleRange.replace("@","").split(":");
            if (value >= Double.parseDouble(values[0]) && value <= Double.parseDouble(values[1])){
                return true;
            }else {return false;}
        }

        if (simpleRange.matches("^-{0,1}[0-9]+:-{0,1}[0-9]+$")){
            String[] values = simpleRange.split(":");
            if (value < Double.parseDouble(values[0]) || value > Double.parseDouble(values[1])){
                return true;

            }else {return false;}
        }

        if (simpleRange.matches("^-{0,1}[0-9]+$") && (0 > value || value > Double.parseDouble(simpleRange))){
                return true;

        }

        return false;

    }

}
