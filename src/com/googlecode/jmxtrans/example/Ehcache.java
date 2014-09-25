package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

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
		GraphiteWriter gw = new GraphiteWriter();
		gw.addSetting(BaseOutputWriter.HOST, GW_HOST);
		gw.addSetting(BaseOutputWriter.PORT, 2003);

		// use this to add data to GW path
		gw.addTypeName("name");

		gw.addSetting(BaseOutputWriter.DEBUG, true);

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

		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.processServer(server);
		// Thread.sleep(1000);
		// }

	}
}
