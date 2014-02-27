package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
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
 * Created by Balazs Kossovics <bko@witbe.net>
 * Date: 4/4/13
 * Time: 6:00 PM
 */
public class OpenTSDBWriter extends BaseOutputWriter {
    public static final boolean DEFAULT_MERGE_TYPE_NAMES_TAGS = true;

    private static final Logger log = LoggerFactory.getLogger(OpenTSDBWriter.class);

    private String host;
    private Integer port;
    private Map<String, String> tags;
    private String tagName;
    private Socket socket;
    private boolean mergeTypeNamesTags = DEFAULT_MERGE_TYPE_NAMES_TAGS;

    String addTags(String resultString) throws UnknownHostException {
        resultString = addTag(resultString, "host", java.net.InetAddress.getLocalHost().getHostName());
        if (tags != null)
            for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
                resultString = addTag(resultString, tagEntry.getKey(), tagEntry.getValue());
            }
        return resultString;
    }

    String addTag(String resultString, String tagName, String tagValue) {
        String tagFormat = " %s=%s";
        resultString += String.format(tagFormat, tagName, tagValue);
        return resultString;
    }

    String getResultString(String className, String attributeName, long epoch, Object value) {
        String resultStringFormat = "put %s.%s %d %s";
        return String.format(resultStringFormat, className, attributeName, epoch, value);
    }

    String getResultString(String className, String attributeName, long epoch, Object value, String tagName, String tagValue) {
        String taggedResultStringFormat = "put %s.%s %d %s %s=%s";
        return String.format(taggedResultStringFormat, className, attributeName, epoch, value, tagName, tagValue);
    }

    List<String> resultParser(Result result) throws UnknownHostException {
        List<String> resultStrings = new LinkedList<String>();
        Map<String, Object> values = result.getValues();
        if (values == null)
            return resultStrings;

        String attributeName = result.getAttributeName();
        String className = result.getClassNameAlias() == null ? result.getClassName() : result.getClassNameAlias();
        if (values.containsKey(attributeName) && values.size() == 1) {
            String resultString = getResultString(className, attributeName, (long)(result.getEpoch()/1000L), values.get(attributeName));
            resultString = addTags(resultString);
            if (getTypeNames().size() > 0) {
                resultString = this.addTypeNamesTags(result, resultString);
            }
            resultStrings.add(resultString);
        } else {
            for (Map.Entry<String, Object> valueEntry: values.entrySet() ) {
                String resultString = getResultString(className, attributeName, (long)(result.getEpoch()/1000L), valueEntry.getValue(), tagName, valueEntry.getKey());
                resultString = addTags(resultString);
                if (getTypeNames().size() > 0) {
                    resultString = this.addTypeNamesTags(result, resultString);
                }
                resultStrings.add(resultString);
            }
        }
        return resultStrings;
    }

    /**
     * Add the tags for the TypeNames setting to the given result string.
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

    @Override
    public void doWrite(Query query) throws Exception {
        DataOutputStream out;
        try {
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log.error("error getting the output stream", e);
            throw e;
        }

        for (Result result : query.getResults()) {
            for(String resultString: resultParser(result)) {
                if (isDebugEnabled())
                    System.out.println(resultString);
                try {
                    out.writeBytes(resultString + "\n"); 
                } catch (IOException e) {
                    log.error("error writing result to the output stream", e);
                    throw e;
                }
            }
        }
        try {
            out.flush();
        } catch (IOException e) {
            log.error("flush failed");
            throw e;
        }
        InputStreamReader socketInputStream = new InputStreamReader(socket.getInputStream());
        BufferedReader bufferedSocketInputStream = new BufferedReader(socketInputStream);
        String line;
        while (socketInputStream.ready() && (line = bufferedSocketInputStream.readLine()) != null) {
            log.warn("OpenTSDB says: " + line); 
        }
    }

    @Override
    public void validateSetup(Query query) throws ValidationException {
    }

    @Override
    public void start() throws LifecycleException {
        host = (String) this.getSettings().get(HOST);
        port = (Integer) this.getSettings().get(PORT);
        tags = (Map<String, String>) this.getSettings().get("tags");
        tagName = this.getStringSetting("tagName", "type");
        mergeTypeNamesTags = this.getBooleanSetting("mergeTypeNamesTags", DEFAULT_MERGE_TYPE_NAMES_TAGS);

        try {
            socket = new Socket(host, port);
        } catch(UnknownHostException e) {
            log.error("error opening socket to OpenTSDB", e);
            throw new LifecycleException(e);
        } catch(IOException e) {
            log.error("error opening socket to OpenTSDB", e);
            throw new LifecycleException(e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            socket.close();
        } catch (IOException e) {
            log.error("error closing socket to OpenTSDB");
            throw new LifecycleException(e);
        }
    }
}
