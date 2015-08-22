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
 * Shows how to connect to a local process.
 * 
 * @author henri
 */
public class Local {

	/**
     *
     */
	public static void main(String[] args) throws Exception {
		JmxProcess process = JsonUtils.getJmxProcess(new File("local.json"));
		new JsonPrinter(System.out).print(process);
		Injector injector = Guice.createInjector(new JmxTransModule(new JmxTransConfiguration()));
		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);
		transformer.executeStandalone(process);

		System.out.println("done!");
	}
}
