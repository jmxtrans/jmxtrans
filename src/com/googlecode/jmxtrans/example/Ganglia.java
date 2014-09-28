package com.googlecode.jmxtrans.example;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GangliaWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

/**
 * This class hits a Graphite server running on port 2003 and sends the memory
 * usage data to it for graphing.
 * 
 * @author jon
 */
public class Ganglia {

	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {
		Server.Builder serverBuilder = Server.builder()
				.setHost("w2")
				.setPort("1099")
				.setAlias("fooalias");

		GangliaWriter gw = new GangliaWriter();
		gw.addSetting(GangliaWriter.HOST, "10.0.3.16");
		gw.addSetting(GangliaWriter.PORT, 8649);
		gw.addSetting(GangliaWriter.DEBUG, true);
		gw.addSetting(GangliaWriter.GROUP_NAME, "memory");

		Query q = Query.builder()
				.setObj("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q);

		JmxProcess process = new JmxProcess(serverBuilder.build());
		printer.prettyPrint(process);

		Injector injector = Guice.createInjector(new JmxTransModule(null));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);
	}

}
