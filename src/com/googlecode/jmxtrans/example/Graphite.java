package com.googlecode.jmxtrans.example;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * This class hits a Graphite server running on port 2003 and sends the memory
 * usage data to it for graphing.
 * 
 * @author jon
 */
public class Graphite {

	private static JsonPrinter printer = new JsonPrinter(System.out);

	public static void main(String[] args) throws Exception {

		Map<String, Object> settings = newHashMap();
		settings.put(GraphiteWriter.HOST, "192.168.192.133");
		settings.put(GraphiteWriter.PORT, 2003);
		settings.put(GraphiteWriter.DEBUG, true);
		settings.put(GraphiteWriter.ROOT_PREFIX, "jon.foo.bar");

		GraphiteWriter gw = new GraphiteWriter(ImmutableList.<String>of(), false, settings);

		Query q = Query.builder()
				.setObj("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep")
				.addOutputWriter(gw)
				.build();

		Server server = Server.builder()
				.setHost("w2")
				.setPort("1099")
				.addQuery(q)
				.build();

		JmxProcess process = new JmxProcess(server);
		printer.prettyPrint(process);
		// JmxTransformer transformer = new JmxTransformer();
		// transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.execute(jmx);
		// Thread.sleep(1000);
		// }
	}

}
