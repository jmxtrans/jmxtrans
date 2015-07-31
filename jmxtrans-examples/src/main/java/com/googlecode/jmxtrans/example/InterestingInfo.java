package com.googlecode.jmxtrans.example;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * This class produces the json that is in example.json.
 * 
 * @author jon
 */
public class InterestingInfo {

	private static final JsonPrinter printer = new JsonPrinter(System.out);

	public static void main(String[] args) throws Exception {
		Server.Builder serverBuilder = Server.builder()
				.setHost("w2")
				.setPort("1099")
				.setNumQueryThreads(2);

		Map<String, Object> settings = newHashMap();
		settings.put(GraphiteWriter.HOST, "192.168.192.133");
		settings.put(GraphiteWriter.PORT, 2003);

		GraphiteWriter gw = GraphiteWriter.builder()
				.setHost("192.168.192.133")
				.setPort(2003)
				.build();

		StdOutWriter sw = new StdOutWriter(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap());

		Query q = Query.builder()
				.setObj("java.lang:type=Memory")
				.addAttr("HeapMemoryUsage")
				.addAttr("NonHeapMemoryUsage")
				.addOutputWriters(gw, sw)
				.build();
		serverBuilder.addQuery(q);

		Query q2 = Query.builder()
				.setObj("java.lang:type=Threading")
				.addAttr("DaemonThreadCount")
				.addAttr("PeakThreadCount")
				.addAttr("ThreadCount")
				.addOutputWriters(gw, sw)
				.build();
		serverBuilder.addQuery(q2);

		Query q3 = Query.builder()
				.setObj("java.lang:name=ConcurrentMarkSweep,type=GarbageCollector")
				.addAttr("LastGcInfo")
				.addOutputWriters(gw, sw)
				.build();
		serverBuilder.addQuery(q3);

		Query q4 = Query.builder()
				.setObj("java.lang:name=ParNew,type=GarbageCollector")
				.addAttr("LastGcInfo")
				.addOutputWriters(gw, sw)
				.build();
		serverBuilder.addQuery(q4);

		JmxProcess process = new JmxProcess(serverBuilder.build());
		printer.prettyPrint(process);
		Injector injector = Guice.createInjector(new JmxTransModule(new JmxTransConfiguration()));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);

		transformer.executeStandalone(process);
	}

}
