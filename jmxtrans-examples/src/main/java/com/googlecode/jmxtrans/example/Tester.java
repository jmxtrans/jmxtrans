/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
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
