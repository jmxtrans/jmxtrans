package com.googlecode.jmxtrans.example;

import java.io.File;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.util.JmxUtils;

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

		JmxProcess process = JmxUtils.getJmxProcess(new File("memorypool.json"));
		JmxUtils.printJson(process);
		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.execute(jmx);
		// Thread.sleep(1000);
		// }

		System.out.println("done!");
	}
}
