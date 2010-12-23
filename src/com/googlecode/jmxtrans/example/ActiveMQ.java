package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This example shows how to query an ActiveMQ server for some information.
 * 
 * The point of this example is to show that * works as part of the objectName.
 * It also shows that you don't have to set an attribute to get for a query.
 * jmxtrans will get all attributes on an object if you don't specify any.
 * 
 * @author jon
 */
public class ActiveMQ {

    /** */
    public static void main(String[] args) throws Exception {

        Server server = new Server("w2", "1105");
        server.setAlias("w2_activemq_1105");
        GraphiteWriter gw = new GraphiteWriter();
        gw.addSetting(GraphiteWriter.HOST, "192.168.192.133");
        gw.addSetting(GraphiteWriter.PORT, 2003);
        gw.addSetting(GraphiteWriter.DEBUG, true);

        Query q = new Query();
        q.setObj("org.apache.activemq:BrokerName=localhost,Type=Broker");
        q.addOutputWriter(gw);
        server.addQuery(q);
        JmxProcess process = new JmxProcess(server);

        JmxUtils.prettyPrintJson(process);
        JmxUtils.processServer(server);

//        for (int i = 0; i < 160; i++) {
//            JmxUtils.processServer(server);
//            Thread.sleep(1000);
//        }

    }
}
