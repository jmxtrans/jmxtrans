package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

public class Tester {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Server server = new Server("w2", "1099");
        server.setNumQueryThreads(2);

        Query q = new Query();
        q.setObj("java.lang:type=Memory");
        q.addAttr("HeapMemoryUsage");
        q.addAttr("NonHeapMemoryUsage");
//        q.addKey("committed");
        q.addOutputWriter(new StdOutWriter());
        server.addQuery(q);

        Query q2 = new Query("java.lang:name=CMS Old Gen,type=MemoryPool");
        q2.addAttr("Usage");
        q2.addKey("committed");
        q2.addOutputWriter(new StdOutWriter());
        server.addQuery(q2);

        Query q3 = new Query();
        q3.setObj("java.lang:name=ConcurrentMarkSweep,type=GarbageCollector");
        q3.addAttr("LastGcInfo");
        q3.addKey("startTime");
        q3.addKey("memoryUsageAfterGc");
        q3.addKey("max");
        q3.addOutputWriter(new StdOutWriter());
        server.addQuery(q3);

        JmxProcess jmx = new JmxProcess(server);
        JmxUtils.prettyPrintJson(jmx);
        JmxUtils.execute(jmx);
    }

}
