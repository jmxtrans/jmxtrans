package com.googlecode.jmxtrans.example;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.util.JsonPrinter;
import com.googlecode.jmxtrans.util.JsonUtils;

import java.io.File;

/**
 * Shows how to process a file.
 * 
 * @author jon
 */
public class MemoryPool {

	/**
     *
     */
	public static void main(String[] args) throws Exception {

		JmxProcess process = JsonUtils.getJmxProcess(new File("memorypool.json"));
		new JsonPrinter(System.out).print(process);
		Injector injector = Guice.createInjector(new JmxTransModule(new JmxTransConfiguration()));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.execute(jmx);
		// Thread.sleep(1000);
		// }

		System.out.println("done!");
	}
}
