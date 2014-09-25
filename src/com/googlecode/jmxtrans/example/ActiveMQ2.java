package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.RRDToolWriter;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
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
public class ActiveMQ2 {

	private static final JsonPrinter jsonPrinter = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {

		Server.Builder serverBuilder = Server.builder()
				.setHost("w2")
				.setPort("1105")
				.setAlias("w2_activemq_1105");

		RRDToolWriter gw = new RRDToolWriter();
		gw.addSetting(BaseOutputWriter.TEMPLATE_FILE, "memorypool-rrd-template.xml");
		gw.addSetting(BaseOutputWriter.OUTPUT_FILE, "target/w2-TEST.rrd");
		gw.addSetting(BaseOutputWriter.BINARY_PATH, "/opt/local/bin");
		gw.addSetting(BaseOutputWriter.DEBUG, "true");
		gw.addSetting(RRDToolWriter.GENERATE, "true");

		// use this to add data to GW path
		gw.addTypeName("Destination");

		Query q = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Type=Queue,Destination=*")
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
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q);

		Query q2 = Query.builder()
				.setObj("org.apache.activemq:BrokerName=localhost,Type=Topic,Destination=*")
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
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q2);

		JmxProcess process = new JmxProcess(serverBuilder.build());
		jsonPrinter.prettyPrint(process);

		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.processServer(server);
		// Thread.sleep(1000);
		// }

	}
}
