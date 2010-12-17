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
public class InterestingInfo {

    /** */
    public static void main(String[] args) throws Exception {
        Server server = new Server("w2", "1099");
        server.setNumQueryThreads(2);

        GraphiteWriter gw = new GraphiteWriter();
        gw.addSetting(GraphiteWriter.HOST, "localhost");
        gw.addSetting(GraphiteWriter.PORT, 2003);
        
        Query q = new Query();
        q.setObj("java.lang:type=Memory");
        q.addAttr("HeapMemoryUsage");
        q.addAttr("NonHeapMemoryUsage");
        q.addOutputWriter(gw);
        server.addQuery(q);

        Query q2 = new Query("java.lang:type=Threading");
        q2.addAttr("DaemonThreadCount");
        q2.addAttr("PeakThreadCount");
        q2.addAttr("ThreadCount");
        q2.addAttr("ThreadCount");
        q2.addOutputWriter(gw);
        server.addQuery(q2);

        Query q3 = new Query();
        q3.setObj("java.lang:name=ConcurrentMarkSweep,type=GarbageCollector");
        q3.addAttr("LastGcInfo");
        q3.addKey("memoryUsageAfterGc");
        q3.addKey("memoryUsageBeforeGc");
        q3.addOutputWriter(gw);
        server.addQuery(q3);

        Query q4 = new Query();
        q3.setObj("java.lang:name=ParNew,type=GarbageCollector");
        q3.addAttr("LastGcInfo");
        q3.addKey("memoryUsageAfterGc");
        q3.addKey("memoryUsageBeforeGc");
        q3.addOutputWriter(gw);
        server.addQuery(q4);

        JmxProcess jmx = new JmxProcess(server);
        JmxUtils.prettyPrintJson(jmx);
//        JmxUtils.execute(jmx);
    }

}
