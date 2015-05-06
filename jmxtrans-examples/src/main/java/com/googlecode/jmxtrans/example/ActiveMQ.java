package com.googlecode.jmxtrans.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

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

	public static void main(String[] args) throws Exception {

		Server.Builder serverBuilder = Server.builder()
				.setHost("w2")
				.setPort("1105")
				.setAlias("w2_activemq_1105");

		GraphiteWriter gw = GraphiteWriter.builder()
				.addTypeName("destinationName")
				.addTypeName("Destination")
				.setDebugEnabled(true)
				.setHost(GW_HOST)
				.setPort(2003)
				.build();

		Query q = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Type=Subscription,clientId=*,consumerId=*,destinationName=*,destinationType=Queue,persistentMode=Non-Durable")
				.addAttr("PendingQueueSize")
				.addAttr("DispatchedQueueSize")
				.addAttr("EnqueueCounter")
				.addAttr("DequeueCounter")
				.addAttr("MessageCountAwaitingAcknowledge")
				.addAttr("DispachedCounter")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q);

		Query q2 = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Destination=ActiveMQ.Advisory.Consumer.Queue.*,Type=Topic")
				.addAttr("QueueSize")
				.addAttr("MaxEnqueueTime")
				.addAttr("MinEnqueueTime")
				.addAttr("AverageEnqueueTime")
				.addAttr("InFlightCount")
				.addAttr("ConsumerCount")
				.addAttr("ProducerCount")
				.addAttr("DispatchCount")
				.addAttr("DequeueCount")
				.addAttr("EnqueueCount")
				.addAttr("Subscriptions")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q2);

		Query q3 = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Destination=*,Type=Queue")
				.addAttr("QueueSize")
				.addAttr("MaxEnqueueTime")
				.addAttr("MinEnqueueTime")
				.addAttr("AverageEnqueueTime")
				.addAttr("InFlightCount")
				.addAttr("ConsumerCount")
				.addAttr("ProducerCount")
				.addAttr("DispatchCount")
				.addAttr("DequeueCount")
				.addAttr("EnqueueCount")
				.addAttr("Subscriptions")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q3);

		Query q4 = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Destination=*,Type=Topic")
				.addAttr("QueueSize")
				.addAttr("MaxEnqueueTime")
				.addAttr("MinEnqueueTime")
				.addAttr("AverageEnqueueTime")
				.addAttr("InFlightCount")
				.addAttr("ConsumerCount")
				.addAttr("ProducerCount")
				.addAttr("DispatchCount")
				.addAttr("DequeueCount")
				.addAttr("EnqueueCount")
				.addAttr("Subscriptions")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q4);

		Query q5 = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Type=Broker")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q5);

		Query q6 = Query.builder()
				.setObj("java.lang:type=Memory")
				.addAttr("HeapMemoryUsage")
				.addAttr("NonHeapMemoryUsage")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q6);

		Query q7 = Query.builder()
				.setObj("java.lang:type=Threading")
				.addAttr("DaemonThreadCount")
				.addAttr("PeakThreadCount")
				.addAttr("ThreadCount")
				.addAttr("CurrentThreadCpuTime")
				.addAttr("CurrentThreadUserTime")
				.addAttr("TotalStartedThreadCount")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q7);

		Query q8 = Query.builder()
				.setObj("java.lang:name=*,type=GarbageCollector")
				.addKey("committed")
				.addKey("init")
				.addKey("max")
				.addKey("used")
				.addKey("duration")
				.addKey("CollectionCount")
				.addKey("CollectionTime")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q8);


		Query q9 = Query.builder()
				.setObj("java.lang:type=MemoryPool,name=*")
				.addOutputWriter(GraphiteWriter.builder()
						.addTypeName("name")
						.setDebugEnabled(true)
						.setHost(GW_HOST)
						.setPort(2003)
						.build())
				.build();
		serverBuilder.addQuery(q9);

		JmxProcess process = new JmxProcess(serverBuilder.build());
		new JsonPrinter(System.out).prettyPrint(process);

		Injector injector = Guice.createInjector(new JmxTransModule(new JmxTransConfiguration()));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.processServer(server);
		// Thread.sleep(1000);
		// }

	}
}
