package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This example shows how to query hibernate for its statistics information.
 *
 * @author jon
 */
public class Hibernate {

	private static final String GW_HOST = "192.168.192.133";

	/** */
	public static void main(String[] args) throws Exception {

		Server server = new Server("w2", "1099");
		server.setAlias("w2_hibernate_1099");
		GraphiteWriter gw = new GraphiteWriter();
		gw.addSetting(BaseOutputWriter.HOST, GW_HOST);
		gw.addSetting(BaseOutputWriter.PORT, 2003);

		// use this to add data to GW path
		gw.addTypeName("name");

		gw.addSetting(BaseOutputWriter.DEBUG, true);

		Query q = new Query();
		q.setObj("org.hibernate.jmx:name=*,type=StatisticsService");
		q.addAttr("EntityDeleteCount");
		q.addAttr("EntityInsertCount");
		q.addAttr("EntityLoadCount");
		q.addAttr("EntityFetchCount");
		q.addAttr("EntityUpdateCount");
		q.addAttr("QueryExecutionCount");
		q.addAttr("QueryCacheHitCount");
		q.addAttr("QueryExecutionMaxTime");
		q.addAttr("QueryCacheMissCount");
		q.addAttr("QueryCachePutCount");
		q.addAttr("FlushCount");
		q.addAttr("ConnectCount");
		q.addAttr("SecondLevelCacheHitCount");
		q.addAttr("SecondLevelCacheMissCount");
		q.addAttr("SecondLevelCachePutCount");
		q.addAttr("SessionCloseCount");
		q.addAttr("SessionOpenCount");
		q.addAttr("CollectionLoadCount");
		q.addAttr("CollectionFetchCount");
		q.addAttr("CollectionUpdateCount");
		q.addAttr("CollectionRemoveCount");
		q.addAttr("CollectionRecreateCount");
		q.addAttr("SuccessfulTransactionCount");
		q.addAttr("TransactionCount");
		q.addAttr("CloseStatementCount");
		q.addAttr("PrepareStatementCount");
		q.addAttr("OptimisticFailureCount");
//        q.addOutputWriter(new StdOutWriter());
		q.addOutputWriter(gw);
		server.addQuery(q);

		JmxProcess process = new JmxProcess(server);
		JmxUtils.prettyPrintJson(process);
        JmxTransformer transformer = new JmxTransformer();
        transformer.executeStandalone(process);

//        for (int i = 0; i < 160; i++) {
//            JmxUtils.processServer(server);
//            Thread.sleep(1000);
//        }

	}
}
