package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

/**
 * This class produces the json that is in example.json.
 * 
 * @author jon
 */
public class Tester {

	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {
		Server server = new Server("w2", "1099");
		server.setNumQueryThreads(2);

		Query q = Query.builder()
				.setObj("java.lang:type=Memory")
				.addAttr("HeapMemoryUsage", "NonHeapMemoryUsage")
				.addOutputWriter(new StdOutWriter())
				.build();
		server.addQuery(q);

		Query q2 = Query.builder()
				.setObj("java.lang:name=CMS Old Gen,type=MemoryPool")
				.addAttr("Usage")
				.addOutputWriter(new StdOutWriter())
				.build();
		server.addQuery(q2);

		Query q3 = Query.builder()
				.setObj("java.lang:name=ConcurrentMarkSweep,type=GarbageCollector")
				.addAttr("LastGcInfo")
				.addOutputWriter(new StdOutWriter())
				.build();
		server.addQuery(q3);

		JmxProcess process = new JmxProcess(server);
		printer.prettyPrint(process);
		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);
	}

}
