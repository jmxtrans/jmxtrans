package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.NamingStrategy;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.naming.ClassAttributeNamingStrategy;
import com.googlecode.jmxtrans.model.naming.JexlNamingStrategy;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import org.apache.commons.jexl2.JexlException;
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
 *
 * Updates by Arthur Naseef
 */
public abstract class OpenTSDBGenericWriter extends BaseOutputWriter {
    public static final boolean DEFAULT_MERGE_TYPE_NAMES_TAGS = true;

    private static final Logger log = LoggerFactory.getLogger(OpenTSDBGenericWriter.class);

    protected String host;
    protected Integer port;
    protected Map<String, String> tags;
    protected String tagName;
    protected NamingStrategy metricNameStrategy;

    protected boolean mergeTypeNamesTags = DEFAULT_MERGE_TYPE_NAMES_TAGS;
    protected boolean addHostnameTag     = getAddHostnameTagDefault();

    /**
     * Prepare for sending metrics, if needed.  For use by subclasses.
     */
    protected void  prepareSender() throws LifecycleException {
    }

    /**
     * Shutdown the sender, if needed.  For use by subclasses.
     */
    protected void  shutdownSender() throws LifecycleException {
    }

    /**
     * Prepare a batch of results output, if needed.  For use by subclasses.
     */
    protected void  startOutput() throws IOException {
    }

    /**
     * Subclass responsibility: specify the default value for the "addHostnameTag" setting.
     */
    protected abstract boolean  getAddHostnameTagDefault();

    /**
     * Subcall responsibility: method to perform the actual output for the given metric line.  Every subclass
     * <b>must</b> implement this method.
     *
     * @param   metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
     *          "put" keyword expected by OpenTSDB and the trailing newline character.
     */
    protected abstract void sendOutput(String metricLine) throws IOException;

    /**
     * Complete a batch of results output, if needed.  For use by subclasses.
     */
    protected void  finishOutput() throws IOException {
    }

    /**
     * Add tags to the given result string, including a "host" tag with the name of the server and all of the tags
     * defined in the "settings" entry in the configuration file within the "tag" element.
     *
     * @param   resultString - the string containing the metric name, timestamp, value, and possibly other content.
     */
    String addTags(String resultString) throws UnknownHostException {
        if ( addHostnameTag ) {
            resultString = addTag(resultString, "host", java.net.InetAddress.getLocalHost().getHostName());
        }

        if (tags != null) {
            // Add the constant tag names and values.
            for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
                resultString = addTag(resultString, tagEntry.getKey(), tagEntry.getValue());
            }
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
        resultString += String.format(tagFormat, sanitizeString(tagName), sanitizeString(tagValue));
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
    String getResultString(String metricName, long epoch, Object value) {
        String resultStringFormat = "%s %d %s";
        return String.format(resultStringFormat, sanitizeString(metricName), epoch, sanitizeString(value.toString()));
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
                String metricName = this.metricNameStrategy.formatName(result);
                String resultString = getResultString(metricName, (long)(result.getEpoch()/1000L), values.get(attributeName));
                resultString = addTags(resultString);
                if (getTypeNames().size() > 0) {
                    resultString = this.addTypeNamesTags(result, resultString);
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
                    String metricName = this.metricNameStrategy.formatName(result);
                    String resultString = getResultString(metricName, (long)(result.getEpoch()/1000L), valueEntry.getValue());
                    resultString = addTag(resultString, tagName, valueEntry.getKey());
                    resultString = addTags(resultString);

                    if (getTypeNames().size() > 0) {
                        resultString = this.addTypeNamesTags(result, resultString);
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
     * Add the tag(s) for typeNames.
     *
     * @param	result - the result of the JMX query.
     * @param	resultString - current form of the metric string.
     * @return	String - the updated metric string with the necessary tag(s) added.
     */
    protected String    addTypeNamesTags (Result result, String resultString) {
        String  retVal = resultString;
        if ( mergeTypeNamesTags ) {
            // Produce a single tag with all the TypeName keys concatenated and all the values joined with '_'.
            retVal = addTag(retVal, StringUtils.join(getTypeNames(), ""), getConcatedTypeNameValues(result.getTypeName()));
        }
        else {
            Map<String, String> typeNameMap = JmxUtils.getTypeNameValueMap(result.getTypeName());
            for ( String oneTypeName : getTypeNames() ) {
                String value = typeNameMap.get(oneTypeName);
                if ( value == null )
                    value = "";
                retVal = addTag(retVal, oneTypeName, value);
            }
        }

        return  retVal;
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

        tagName            = this.getStringSetting("tagName", "type");
        mergeTypeNamesTags = this.getBooleanSetting("mergeTypeNamesTags", DEFAULT_MERGE_TYPE_NAMES_TAGS);
        addHostnameTag     = this.getBooleanSetting("addHostnameTag", this.getAddHostnameTagDefault());

        this.setupNamingStrategies();
        this.prepareSender();
    }

    @Override
    public void stop() throws LifecycleException {
        this.shutdownSender();
    }

    /**
     * Set the naming strategies based on the configuration.
     */
    protected void  setupNamingStrategies () throws LifecycleException {
        try {
            String jexlExpr = this.getStringSetting("metricNamingExpression", null);
            if ( jexlExpr != null ) {
                this.metricNameStrategy = new JexlNamingStrategy(jexlExpr);
            }
            else {
                this.metricNameStrategy = new ClassAttributeNamingStrategy();
            }
        }
        catch ( JexlException jexlExc ) {
            throw   new LifecycleException("failed to setup naming strategy", jexlExc);
        }

    }

    /**
     * VALID CHARACTERS:
     *      METRIC, TAGNAME, AND TAG-VALUE:
     *          [-_./a-zA-Z0-9]+
     *
     *
     * SANITIZATION:
     *      - Discard Quotes.
     *      - Replace all other invalid characters with '_'.
     */
    protected String    sanitizeString (String unsanitized) {
        String  sanitized;

        sanitized =
            unsanitized.
                    replaceAll("[\"']", "").
                    replaceAll("[^-_./a-zA-Z0-9]", "_");

        return  sanitized;
    }
}
