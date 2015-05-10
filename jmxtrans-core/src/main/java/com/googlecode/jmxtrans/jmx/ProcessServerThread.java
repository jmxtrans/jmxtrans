package com.googlecode.jmxtrans.jmx;

import com.googlecode.jmxtrans.model.Server;

import javax.management.remote.JMXConnector;

/**
 * Executes either a getAttribute or getAttributes query.
 */
public class ProcessServerThread implements Runnable {
	private final Server server;
	private final JMXConnector conn;
	private final JmxUtils jmxUtils;

	public ProcessServerThread(Server server, JMXConnector conn, JmxUtils jmxUtils) {
		this.server = server;
		this.conn = conn;
		this.jmxUtils = jmxUtils;
	}

	public void run() {
		try {
			jmxUtils.processServer(this.server, this.conn);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
