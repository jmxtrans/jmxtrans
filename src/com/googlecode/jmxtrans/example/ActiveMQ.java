package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
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
public class ActiveMQ {

	private static final String GW_HOST = "192.168.192.133";

	/** */
	public static void main(String[] args) throws Exception {

		Server server = new Server("w2", "1105");
		server.setAlias("w2_activemq_1105");
		GraphiteWriter gw = new GraphiteWriter();
		gw.addSetting(BaseOutputWriter.HOST, GW_HOST);
//        gw.addSetting(GraphiteWriter.HOST, "localhost");
		gw.addSetting(BaseOutputWriter.PORT, 2003);

		// use this to add data to GW path
		gw.addTypeName("destinationName");
		gw.addTypeName("Destination");
		gw.addSetting(BaseOutputWriter.DEBUG, true);

		Query q = new Query();
		q.setObj("org.apache.activemq:BrokerName=localhost,Type=Subscription,clientId=*,consumerId=*,destinationName=*,destinationType=Queue,persistentMode=Non-Durable");
		q.addAttr("PendingQueueSize");
		q.addAttr("DispatchedQueueSize");
		q.addAttr("EnqueueCounter");
		q.addAttr("DequeueCounter");
		q.addAttr("MessageCountAwaitingAcknowledge");
		q.addAttr("DispachedCounter");
		q.addOutputWriter(gw);
		server.addQuery(q);

		Query q2 = new Query();
		q2.setObj("org.apache.activemq:BrokerName=localhost,Destination=ActiveMQ.Advisory.Consumer.Queue.*,Type=Topic");
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
		q2.addAttr("Subscriptions");
		q2.addOutputWriter(gw);
		server.addQuery(q2);

		Query q3 = new Query();
		q3.setObj("org.apache.activemq:BrokerName=localhost,Destination=*,Type=Queue");
		q3.addAttr("QueueSize");
		q3.addAttr("MaxEnqueueTime");
		q3.addAttr("MinEnqueueTime");
		q3.addAttr("AverageEnqueueTime");
		q3.addAttr("InFlightCount");
		q3.addAttr("ConsumerCount");
		q3.addAttr("ProducerCount");
		q3.addAttr("DispatchCount");
		q3.addAttr("DequeueCount");
		q3.addAttr("EnqueueCount");
		q3.addAttr("Subscriptions");
		q3.addOutputWriter(gw);
		server.addQuery(q3);

		Query q4 = new Query();
		q4.setObj("org.apache.activemq:BrokerName=localhost,Destination=*,Type=Topic");
		q4.addAttr("QueueSize");
		q4.addAttr("MaxEnqueueTime");
		q4.addAttr("MinEnqueueTime");
		q4.addAttr("AverageEnqueueTime");
		q4.addAttr("InFlightCount");
		q4.addAttr("ConsumerCount");
		q4.addAttr("ProducerCount");
		q4.addAttr("DispatchCount");
		q4.addAttr("DequeueCount");
		q4.addAttr("EnqueueCount");
		q4.addAttr("Subscriptions");
		q4.addOutputWriter(gw);
		server.addQuery(q4);

		Query q5 = new Query();
		q5.setObj("org.apache.activemq:BrokerName=localhost,Type=Broker");
		q5.addOutputWriter(gw);
		server.addQuery(q5);

		Query q6 = new Query();
		q6.setObj("java.lang:type=Memory");
		q6.addAttr("HeapMemoryUsage");
		q6.addAttr("NonHeapMemoryUsage");
		q6.addOutputWriter(gw);
		server.addQuery(q6);

		Query q7 = new Query("java.lang:type=Threading");
		q7.addAttr("DaemonThreadCount");
		q7.addAttr("PeakThreadCount");
		q7.addAttr("ThreadCount");
		q7.addAttr("CurrentThreadCpuTime");
		q7.addAttr("CurrentThreadUserTime");
		q7.addAttr("TotalStartedThreadCount");
		q7.addOutputWriter(gw);
		server.addQuery(q7);

		Query q8 = new Query();
		q8.setObj("java.lang:name=*,type=GarbageCollector");
		q8.addKey("committed");
		q8.addKey("init");
		q8.addKey("max");
		q8.addKey("used");
		q8.addKey("duration");
		q8.addKey("CollectionCount");
		q8.addKey("CollectionTime");
		q8.addOutputWriter(gw);
		server.addQuery(q8);

		GraphiteWriter gw2 = new GraphiteWriter();
		gw2.addSetting(BaseOutputWriter.HOST, GW_HOST);
		gw2.addSetting(BaseOutputWriter.PORT, 2003);

		gw2.addTypeName("name");
		gw2.addSetting(BaseOutputWriter.DEBUG, true);

		Query q9 = new Query();
		q9.setObj("java.lang:type=MemoryPool,name=*");
		q9.addOutputWriter(gw2);
		server.addQuery(q9);

		JmxProcess process = new JmxProcess(server);
		JmxUtils.prettyPrintJson(process);
        JmxTransformer transformer = new JmxTransformer();
        transformer.executeStandalone(process);

//        for (int i = 0; i < 160; i++) {
//            JmxUtils.processServer(server);
//            Thread.sleep(1000);
//        }

	}
}
