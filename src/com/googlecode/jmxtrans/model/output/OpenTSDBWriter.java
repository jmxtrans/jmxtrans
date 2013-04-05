package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
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

    @Override
    public void doWrite(Query query) throws Exception {
        System.out.println(host);
        System.out.println(port);
        System.out.println(tags);
        Socket socket = new Socket(host, port);
        DataOutputStream out = new DataOutputStream(
                socket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        for (Result r : query.getResults()) {
            String attributeName = r.getAttributeName();
            Map<String, Object> values = r.getValues();
            if (values != null) {
                String className = r.getClassNameAlias() == null ? r.getClassName() : r.getClassNameAlias();
                if (r.getValues().containsKey(attributeName) && r.getValues().size() == 1) {
                    String m = "put " + className + "." + r.getAttributeName() + " " + (long)(r.getEpoch()/1000L) + " " + r.getValues().get(attributeName);
                    for (Map.Entry<String, String> e: tags.entrySet()) {
                        m += " " + e.getKey() + "=" + e.getValue();
                    }
                    out.writeBytes(m + "\n");
                    System.out.println(m);
                } else {
                    System.out.println(r.getValues().size());
                    for (Map.Entry<String, Object> e : r.getValues().entrySet()) {
                        String m = "put " + className + "." + r.getAttributeName() + " " + (long)(r.getEpoch()/1000L) + " " + e.getValue() + " type=" + e.getKey();
                        for (Map.Entry<String, String> f: tags.entrySet()) {
                            m += " " + f.getKey() + "=" + f.getValue();
                        }
                        out.writeBytes(m + "\n");
                        System.out.println(m);
                    }
                }
                System.out.println(r);
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
