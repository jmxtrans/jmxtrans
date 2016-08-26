/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.example;

import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.RRDToolWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.IOException;

/**
 * This example shows how to query an ActiveMQ server for some information.
 * 
 * The point of this example is to show that * works as part of the objectName.
 * It also shows that you don't have to set an attribute to get for a query.
 * jmxtrans will get all attributes on an object if you don't specify any.
 * 
 * @author jon
 */
@SuppressWarnings({"squid:S106", "squid:S1118"}) // using StdOut if fine in an example
public class ActiveMQ2 {

	private static final JsonPrinter jsonPrinter = new JsonPrinter(System.out);

	@SuppressFBWarnings(
			value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
			justification = "Path to RRD binary is hardcoded as this is example code")
	public static void main(String[] args) throws Exception {
		File outputFile = new File("target/w2-TEST.rrd");
		if (!outputFile.exists() && !outputFile.createNewFile()) {
			throw new IOException("Could not create output file");
		}
		RRDToolWriter gw = RRDToolWriter.builder()
				.setTemplateFile(new File("memorypool-rrd-template.xml"))
				.setOutputFile(outputFile)
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
						.addOutputWriterFactory(gw)
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
						.addOutputWriterFactory(gw)
						.build()).build());
		jsonPrinter.prettyPrint(process);

		Injector injector = JmxTransModule.createInjector(new JmxTransConfiguration());
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);
	}
}
