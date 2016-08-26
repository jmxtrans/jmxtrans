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
import com.googlecode.jmxtrans.model.output.GraphiteWriter;

/**
 * This example shows how to query ehcache for its statistics information.
 * 
 * @author jon
 */
@SuppressWarnings({"squid:S106", "squid:S1118"}) // using StdOut if fine in an example
public class Ehcache {

	@SuppressWarnings("squid:S1313") // this is an example, no need to make it configurable
	private static final String GW_HOST = "192.168.192.133";
	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {

		JmxProcess process = new JmxProcess(Server.builder()
				.setHost("w2")
				.setPort("1099")
				.setAlias("w2_ehcache_1099")
				.addQuery(Query.builder()
						.setObj("net.sf.ehcache:CacheManager=net.sf.ehcache.CacheManager@*,name=*,type=CacheStatistics")
						.addAttr("CacheHits")
						.addAttr("InMemoryHits")
						.addAttr("OnDiskHits")
						.addAttr("CacheMisses")
						.addAttr("ObjectCount")
						.addAttr("MemoryStoreObjectCount")
						.addAttr("DiskStoreObjectCount")
						.addOutputWriterFactory(GraphiteWriter.builder()
								.addTypeName("name")
								.setDebugEnabled(true)
								.setHost(GW_HOST)
								.setPort(2003)
								.build())
						.build())
				.build());

		printer.prettyPrint(process);

		Injector injector = JmxTransModule.createInjector(new JmxTransConfiguration());
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);

		transformer.executeStandalone(process);
	}
}
