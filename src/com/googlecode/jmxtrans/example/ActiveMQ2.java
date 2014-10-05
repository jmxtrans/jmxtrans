package com.googlecode.jmxtrans.example;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.RRDToolWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.BINARY_PATH;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.DEBUG;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.OUTPUT_FILE;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.TEMPLATE_FILE;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.TYPE_NAMES;
import static java.util.Arrays.asList;

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

		Map<String, Object> settings = newHashMap();
		settings.put(TEMPLATE_FILE, "memorypool-rrd-template.xml");
		settings.put(OUTPUT_FILE, "target/w2-TEST.rrd");
		settings.put(BINARY_PATH, "/opt/local/bin");
		settings.put(DEBUG, "true");
		settings.put(RRDToolWriter.GENERATE, "true");
		settings.put(TYPE_NAMES, asList("Destination"));

		RRDToolWriter gw = new RRDToolWriter(ImmutableList.<String>of(), false, settings);

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

		Injector injector = Guice.createInjector(new JmxTransModule(null));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);
	}
}
