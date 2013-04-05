package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Balazs Kossovics <bko@witbe.net>
 * Date: 4/4/13
 * Time: 6:00 PM
 */
public class OpenTSDBWriter extends BaseOutputWriter {

    private String host;
    private Integer port;
    private Map<String, String> tags;

    String addTags(String resultString) {
        String tagFormat = " %s=%s";
        for (Map.Entry<String, String> tagEntry : tags.entrySet()) {
            resultString += String.format(tagFormat, tagEntry.getKey(), tagEntry.getValue());
        }
        return resultString;
    }

    List<String> resultParser(Result result) {
        List<String> resultStrings = new LinkedList<String>();
        Map<String, Object> values = result.getValues();
        if (values == null)
            return resultStrings;
        String resultStringFormat = "put %s.%s %d %s";
        String taggedResultStringFormat = "put %s.%s %d %s type=%s";

        String attributeName = result.getAttributeName();
        String className = result.getClassNameAlias() == null ? result.getClassName() : result.getClassNameAlias();
        if (values.containsKey(attributeName) && values.size() == 1) {
            String resultString = String.format(resultStringFormat, className, attributeName, (long)(result.getEpoch()/1000L), values.get(attributeName));
            resultString = addTags(resultString);
            resultStrings.add(resultString);
        } else {
            for (Map.Entry<String, Object> valueEntry: values.entrySet() ) {
                String resultString = String.format(taggedResultStringFormat, className, attributeName, (long)(result.getEpoch()/1000L), valueEntry.getValue(), valueEntry.getKey());
                resultString = addTags(resultString);
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

        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        List<String> resultStrings = new LinkedList<String>();
        for (Result result : query.getResults()) {
            for(String resultString: resultParser(result)) {
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

    }
}
