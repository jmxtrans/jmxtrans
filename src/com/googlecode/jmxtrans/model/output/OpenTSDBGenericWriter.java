package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Originally written by Balazs Kossovics <bko@witbe.net>.  Common base class for OpenTSDBWriter and TCollectorWriter.
 * Date: 4/4/13
 * Time: 6:00 PM
 */
public abstract class OpenTSDBGenericWriter extends BaseOutputWriter {
    private static final Logger log = LoggerFactory.getLogger(OpenTSDBGenericWriter.class);

    protected String host;
    protected Integer port;
    protected Map<String, String> tags;
    protected String tagName;

    /**
     * Prepare for sending metrics.
     */
    protected abstract void prepareSender() throws LifecycleException;

    /**
     * Shutdown the sender.
     */
    protected abstract void shutdownSender() throws LifecycleException;

    /**
     * Subcall responsibility: called at the start of a set of results to send.
     */
    protected abstract void startOutput() throws IOException;

    /**
     * Subcall responsibility: method to perform the actual output for the given metric line.
     *
     * @param   metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
     *          "put" keyword expected by OpenTSDB and the trailing newline character.
     */
    protected abstract void sendOutput(String metricLine) throws IOException;

    /**
     * Subcall responsibility: called at the start of a set of results to send.
     */
    protected abstract void finishOutput() throws IOException;


    /**
     * Add tags to the given result string, including a "host" tag with the name of the server and all of the tags
     * defined in the "settings" entry in the configuration file within the "tag" element.
     *
     * @param   resultString - the string containing the metric name, timestamp, value, and possibly other content.
     */
    String addTags(String resultString) throws UnknownHostException {
        resultString = addTag(resultString, "host", java.net.InetAddress.getLocalHost().getHostName());
        if (tags != null)
            for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
                resultString = addTag(resultString, tagEntry.getKey(), tagEntry.getValue());
            }
        return resultString;
    }

    /**
     * Add one tag, with the provided name and value, to the given result string.
     *
     * @param   resultString - the string containing the metric name, timestamp, value, and possibly other content.
     * @return  String - the new result string with the tag appended.
     */
    String addTag(String resultString, String tagName, String tagValue) {

        String tagFormat = " %s=%s";
        resultString += String.format(tagFormat, JmxUtils.cleanupStr(tagName), JmxUtils.cleanupStr(tagValue));
        return resultString;
    }

    /**
     * Format the result string given the class name and attribute name of the source value, the timestamp, and the
     * value.
     *
     * @param   className - the name of the class of the MBean from which the value was sourced.
     * @param   attributeName - the name of the attribute of the MBean from which the value was sourced.  For complex
     *                          types (such as CompositeData), the attribute name may describe a hierarchy.
     * @param   epoch - the timestamp of the metric.
     * @param   value - value of the attribute to use as the metric value.
     * @return  String - the formatted result string.
     */
    String getResultString(String className, String attributeName, long epoch, Object value) {
        String resultStringFormat = "%s.%s %d %s";
        return String.format(resultStringFormat, className, attributeName, epoch, value);
    }

    /**
     * Format the result string given the class name and attribute name of the source value, the timestamp, the value,
     * a tagname, and a tag value.
     *
     * @param   className - the name of the class of the MBean from which the value was sourced.
     * @param   attributeName - the name of the attribute of the MBean from which the value was sourced.  For complex
     *                          types (such as CompositeData), the attribute name may describe a hierarchy.
     * @param   epoch - the timestamp of the metric.
     * @param   value - value of the attribute to use as the metric value.
     * @param   tagName - name of the tag to include.
     * @param   tagValue - value of the tag to include.
     * @return  String - the formatted result string.
     */
    String getResultString(String className, String attributeName, long epoch, Object value, String tagName, String tagValue) {
        String taggedResultStringFormat = "%s.%s %d %s %s=%s";
        return String.format(taggedResultStringFormat, className, attributeName, epoch, value, tagName, tagValue);
    }

    /**
     * Parse one of the results of a Query and return a list of strings containing metric details ready for sending to
     * OpenTSDB.
     *
     * @param   result - one results from the Query.
     * @return  List<String> - the list of strings containing metric details ready for sending to OpenTSDB.
     */
    List<String> resultParser(Result result) throws UnknownHostException {
        List<String> resultStrings = new LinkedList<String>();
        Map<String, Object> values = result.getValues();
        if (values == null)
            return resultStrings;

        String attributeName = result.getAttributeName();
        String className = result.getClassNameAlias() == null ? result.getClassName() : result.getClassNameAlias();
        if (values.containsKey(attributeName) && values.size() == 1) {
            if ( JmxUtils.isNumeric(values.get(attributeName)) ) {
                String resultString = getResultString(className, attributeName, (long)(result.getEpoch()/1000L), values.get(attributeName));
                resultString = addTags(resultString);
                if (getTypeNames().size() > 0) {
                    resultString = addTag(resultString, StringUtils.join(getTypeNames(), ""), getConcatedTypeNameValues(result.getTypeName()));
                }
                resultStrings.add(resultString);
            }
            else {
                log.debug("discarding non-numeric value for attribute {}; value={}", attributeName,
                          values.get(attributeName));
            }
        } else {
            for (Map.Entry<String, Object> valueEntry: values.entrySet() ) {
                if ( JmxUtils.isNumeric(valueEntry.getValue()) ) {
                    String resultString = getResultString(className, attributeName, (long)(result.getEpoch()/1000L), valueEntry.getValue(), tagName, valueEntry.getKey());
                    resultString = addTags(resultString);
                    if (getTypeNames().size() > 0) {
                        resultString = addTag(resultString, StringUtils.join(getTypeNames(), ""), getConcatedTypeNameValues(result.getTypeName()));
                    }
                    resultStrings.add(resultString);
                }
                else {
                    log.debug("discarding non-numeric value for attribute {}; value={}", attributeName,
                              valueEntry.getValue());
                }
            }
        }
        return resultStrings;
    }

    /**
     * Write the results of the query.
     *
     * @param   query - the query and its results.
     */
    @Override
    public void doWrite(Query query) throws Exception {
        this.startOutput();
        for (Result result : query.getResults()) {
            for(String resultString: resultParser(result)) {
                if (isDebugEnabled())
                    System.out.println(resultString);

                this.sendOutput(resultString);
            }
        }
        this.finishOutput();
    }

    @Override
    public void validateSetup(Query query) throws ValidationException {
        if (host == null || port == null) {
            throw new ValidationException("Host and port can't be null", query);
        }
    }

    /** 
     * Start the output writer.  At this time, the settings are read from the configuration file and saved for later
     * use.
     */
    @Override
    public void start() throws LifecycleException {
        host = (String) this.getSettings().get(HOST);
        port = (Integer) this.getSettings().get(PORT);
        tags = (Map<String, String>) this.getSettings().get("tags");
        tagName = this.getStringSetting("tagName", "type");

        this.prepareSender();
    }

    @Override
    public void stop() throws LifecycleException {
        this.shutdownSender();
    }
}
