package com.googlecode.jmxtrans.jmx;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;

/**
 * Executes either a getAttribute or getAttributes query.
 */
public class ProcessQueryThread implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MBeanServerConnection mbeanServer;
	private final Server server;
	private final Query query;
	private final ValueTransformer valueTransformer;

	public ProcessQueryThread(MBeanServerConnection mbeanServer, Server server, Query query, ValueTransformer valueTransformer) {
		this.mbeanServer = mbeanServer;
		this.server = server;
		this.query = query;
		this.valueTransformer = valueTransformer;
	}

	public void run() {
		try {
			new JmxQueryProcessor(valueTransformer).processQuery(this.mbeanServer, this.server, this.query);
		} catch (Exception e) {
			log.error("Error executing query: " + query, e);
			throw new RuntimeException(e);
		}
	}
}
