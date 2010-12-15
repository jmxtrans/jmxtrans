package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This class produces the json that is in example.json.
 * 
 * @author jon
 */
public class Graphite {

    /** */
    public static void main(String[] args) throws Exception {
        Server server = new Server("w2", "1099");

        Query q = new Query();
        q.setObj("java.lang:type=Memory");
        q.addAttr("HeapMemoryUsage");
        q.addAttr("NonHeapMemoryUsage");
        GraphiteWriter gw = new GraphiteWriter();
        gw.setHost("192.168.192.133");
        gw.setPort(2003);
        q.addOutputWriter(gw);
        server.addQuery(q);

        JmxProcess jmx = new JmxProcess(server);
        
        for (int i = 0; i < 160; i++) {
            JmxUtils.execute(jmx);
            Thread.sleep(1000);
        }
    }

}
