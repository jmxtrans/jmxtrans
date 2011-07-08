package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GangliaWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This class hits a Graphite server running on port 2003
 * and sends the memory usage data to it for graphing.
 *
 * @author jon
 */
public class Ganglia {

    /** */
    public static void main(String[] args) throws Exception {
        Server server = new Server("w2", "1099");
        server.setAlias("fooalias");

        Query q = new Query();
        q.setObj("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep");

        GangliaWriter gw = new GangliaWriter();
        gw.addSetting(GangliaWriter.HOST, "10.0.3.16");
        gw.addSetting(GangliaWriter.PORT, 8649);
        gw.addSetting(GangliaWriter.DEBUG, true);
        gw.addSetting(GangliaWriter.GROUP_NAME, "memory");
        q.addOutputWriter(gw);
        server.addQuery(q);

        JmxProcess process = new JmxProcess(server);
        JmxUtils.prettyPrintJson(process);

        JmxTransformer transformer = new JmxTransformer();
        transformer.executeStandalone(process);

//        for (int i = 0; i < 160; i++) {
//            JmxUtils.execute(jmx);
//            Thread.sleep(1000);
//        }
    }

}
