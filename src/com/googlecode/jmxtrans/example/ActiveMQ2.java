package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.RRDToolWriter;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
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
public class ActiveMQ2 {

	/** */
	public static void main(String[] args) throws Exception {

		Server server = new Server("w2", "1105");
		server.setAlias("w2_activemq_1105");
		RRDToolWriter gw = new RRDToolWriter();
		gw.addSetting(BaseOutputWriter.TEMPLATE_FILE, "memorypool-rrd-template.xml");
		gw.addSetting(BaseOutputWriter.OUTPUT_FILE, "target/w2-TEST.rrd");
		gw.addSetting(BaseOutputWriter.BINARY_PATH, "/opt/local/bin");
		gw.addSetting(BaseOutputWriter.DEBUG, "true");
		gw.addSetting(RRDToolWriter.GENERATE, "true");

		// use this to add data to GW path
		gw.addTypeName("Destination");

		Query q = new Query();
		q.setObj("org.apache.activemq:BrokerName=localhost,Type=Queue,Destination=*");
		q.addAttr("QueueSize");
		q.addAttr("MaxEnqueueTime");
		q.addAttr("MinEnqueueTime");
		q.addAttr("AverageEnqueueTime");
		q.addAttr("InFlightCount");
		q.addAttr("ConsumerCount");
		q.addAttr("ProducerCount");
		q.addAttr("DispatchCount");
		q.addAttr("DequeueCount");
		q.addAttr("EnqueueCount");
		q.addOutputWriter(gw);
		server.addQuery(q);

		Query q2 = new Query();
		q2.setObj("org.apache.activemq:BrokerName=localhost,Type=Topic,Destination=*");
		q2.addAttr("QueueSize");
		q2.addAttr("MaxEnqueueTime");
		q2.addAttr("MinEnqueueTime");
		q2.addAttr("AverageEnqueueTime");
		q2.addAttr("InFlightCount");
		q2.addAttr("ConsumerCount");
		q2.addAttr("ProducerCount");
		q2.addAttr("DispatchCount");
		q2.addAttr("DequeueCount");
		q2.addAttr("EnqueueCount");
		q2.addOutputWriter(gw);
		server.addQuery(q2);

		JmxProcess process = new JmxProcess(server);
		JmxUtils.prettyPrintJson(process);
		JmxUtils.execute(process);

//        for (int i = 0; i < 160; i++) {
//            JmxUtils.processServer(server);
//            Thread.sleep(1000);
//        }

	}
}
