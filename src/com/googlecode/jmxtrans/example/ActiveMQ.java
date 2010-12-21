package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
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

        Server server = new Server("localhost", "1099");

        Query q = new Query();
        q.setObj("org.apache.activemq:BrokerName=localhost,Connection=*,ConnectorName=openwire,Type=Connection");
        q.addOutputWriter(new StdOutWriter());
        server.addQuery(q);

        JmxUtils.processServer(server);
    }
}
