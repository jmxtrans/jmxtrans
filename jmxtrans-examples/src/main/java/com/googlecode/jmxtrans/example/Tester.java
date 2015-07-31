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
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

import java.util.Collections;

/**
 * This class produces the json that is in example.json.
 * 
 * @author jon
 */
public class Tester {

	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {
		Server server = Server.builder()
				.setHost("w2")
				.setPort("1099")
				.setNumQueryThreads(2)
				.addQuery(Query.builder()
						.setObj("java.lang:type=Memory")
						.addAttr("HeapMemoryUsage", "NonHeapMemoryUsage")
						.addOutputWriter(new StdOutWriter(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap()))
						.build())
				.addQuery(Query.builder()
						.setObj("java.lang:name=CMS Old Gen,type=MemoryPool")
						.addAttr("Usage")
						.addOutputWriter(new StdOutWriter(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap()))
						.build())
				.addQuery(Query.builder()
						.setObj("java.lang:name=ConcurrentMarkSweep,type=GarbageCollector")
						.addAttr("LastGcInfo")
						.addOutputWriter(new StdOutWriter(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap()))
						.build())
				.build();

		JmxProcess process = new JmxProcess(server);
		printer.prettyPrint(process);

		Injector injector = Guice.createInjector(new JmxTransModule(new JmxTransConfiguration()));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);

		transformer.executeStandalone(process);
	}

}
