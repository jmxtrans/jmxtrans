package com.googlecode.jmxtrans.example;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.DEBUG;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.HOST;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.PORT;

/**
 * This example shows how to query ehcache for its statistics information.
 * 
 * @author jon
 */
public class Ehcache {

	private static final String GW_HOST = "192.168.192.133";
	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {

		Server.Builder serverBuilder = Server.builder()
				.setHost("w2")
				.setPort("1099")
				.setAlias("w2_ehcache_1099");

		Map<String, Object> settings = newHashMap();
		settings.put(HOST, GW_HOST);
		settings.put(PORT, 2003);
		settings.put(DEBUG, true);

		GraphiteWriter gw = new GraphiteWriter(ImmutableList.<String>of("name"), false, settings);

		Query q = Query.builder()
				.setObj("net.sf.ehcache:CacheManager=net.sf.ehcache.CacheManager@*,name=*,type=CacheStatistics")
				.addAttr("CacheHits")
				.addAttr("InMemoryHits")
				.addAttr("OnDiskHits")
				.addAttr("CacheMisses")
				.addAttr("ObjectCount")
				.addAttr("MemoryStoreObjectCount")
				.addAttr("DiskStoreObjectCount")
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
