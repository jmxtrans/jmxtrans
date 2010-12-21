package com.googlecode.jmxtrans.example;

import java.io.File;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * Shows how to process a file.
 * @author jon
 */
public class HeapMemory {

    /**
     * 
     */
    public static void main(String[] args) throws Exception {

        JmxProcess jmx = JmxUtils.getJmxProcess(new File("heapmemory.json"));
        JmxUtils.printJson(jmx);
        
        for (int i = 0; i < 160; i++) {
            JmxUtils.execute(jmx);
            Thread.sleep(1000);
        }

        System.out.println("done!");
    }
}
