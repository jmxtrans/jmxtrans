package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

/**
 * This class hits a Graphite server running on port 2003 and sends the memory
 * usage data to it for graphing.
 * 
 * @author jon
 */
public class Graphite {

	private static JsonPrinter printer = new JsonPrinter(System.out);

	public static void main(String[] args) throws Exception {
		printer.prettyPrint(new JmxProcess(Server.builder()
				.setHost("w2")
				.setPort("1099")
				.addQuery(Query.builder()
						.setObj("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep")
						.addOutputWriter(GraphiteWriter.builder()
								.setHost("192.168.192.133")
								.setPort(2003)
								.setDebugEnabled(true)
								.setRootPrefix("jon.foo.bar")
								.build())
						.build())
				.build()));
	}

}
