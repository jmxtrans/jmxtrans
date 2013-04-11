package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
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

/**
 * Created by Balazs Kossovics <bko@witbe.net>
 * Date: 4/4/13
 * Time: 6:00 PM
 */
public class OpenTSDBWriter extends BaseOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(OpenTSDBWriter.class);

    private String host;
    private Integer port;
    private Map<String, String> tags;
    private String tagName;

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
                resultString = addTag(resultString, StringUtils.join(getTypeNames(), ""), getConcatedTypeNameValues(result.getTypeName()));
            }
            resultStrings.add(resultString);
        } else {
            for (Map.Entry<String, Object> valueEntry: values.entrySet() ) {
                String resultString = getResultString(className, attributeName, (long)(result.getEpoch()/1000L), valueEntry.getValue(), tagName, valueEntry.getKey());
                resultString = addTags(resultString);
                if (getTypeNames().size() > 0) {
                    resultString = addTag(resultString, StringUtils.join(getTypeNames(), ""), getConcatedTypeNameValues(result.getTypeName()));
                }
                resultStrings.add(resultString);
            }
        }
        return resultStrings;
    }

    @Override
    public void doWrite(Query query) throws Exception {
        Socket socket = new Socket(host, port);
        DataOutputStream out = new DataOutputStream(
                socket.getOutputStream());

        for (Result result : query.getResults()) {
            for(String resultString: resultParser(result)) {
                if (isDebugEnabled())
                    System.out.println(resultString);
                out.writeBytes(resultString + "\n");
            }
        }

        socket.close();
    }

    @Override
    public void validateSetup(Query query) throws ValidationException {
        host = (String) this.getSettings().get(HOST);
        port = (Integer) this.getSettings().get(PORT);
        tags = (Map<String, String>) this.getSettings().get("tags");
        tagName = this.getStringSetting("tagName", "type");
    }
}
