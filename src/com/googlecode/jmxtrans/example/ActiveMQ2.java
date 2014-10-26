package com.googlecode.jmxtrans.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.RRDToolWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;

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

	@SuppressFBWarnings(
			value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
			justification = "Path to RRD binary is hardcoded as this is example code")
	public static void main(String[] args) throws Exception {
		RRDToolWriter gw = RRDToolWriter.builder()
				.setTemplateFile(new File("memorypool-rrd-template.xml"))
				.setOutputFile(new File("target/w2-TEST.rrd"))
				.setBinaryPath(new File("/opt/local/bin"))
				.setDebugEnabled(true)
				.setGenerate(true)
				.addTypeName("Destination")
				.build();

		JmxProcess process = new JmxProcess(Server.builder()
				.setHost("w2")
				.setPort("1105")
				.setAlias("w2_activemq_1105")
				.addQuery(Query.builder()
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
						.build())
				.addQuery(Query.builder()
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
						.build()).build());
		jsonPrinter.prettyPrint(process);

		Injector injector = Guice.createInjector(new JmxTransModule(null));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);
	}
}
