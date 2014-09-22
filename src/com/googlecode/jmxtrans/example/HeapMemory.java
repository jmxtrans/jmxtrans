package com.googlecode.jmxtrans.example;

import java.io.File;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.util.JsonPrinter;
import com.googlecode.jmxtrans.util.JsonUtils;

/**
 * Shows how to process a file.
 * 
 * @author jon
 */
public class HeapMemory {

	/**
     *
     */
	public static void main(String[] args) throws Exception {

		JmxProcess process = JsonUtils.getJmxProcess(new File("heapmemory.json"));
		new JsonPrinter(System.out).print(process);

		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.execute(jmx);
		// Thread.sleep(1000);
		// }

		System.out.println("done!");
	}
}
