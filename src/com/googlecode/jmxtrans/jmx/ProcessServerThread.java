package com.googlecode.jmxtrans.jmx;

import javax.management.remote.JMXConnector;

import com.googlecode.jmxtrans.model.Server;

/**
 * Executes either a getAttribute or getAttributes query.
 */
public class ProcessServerThread implements Runnable {
	private Server server;
	private JMXConnector conn;

	public ProcessServerThread(Server server, JMXConnector conn) {
		this.server = server;
		this.conn = conn;
	}

	public void run() {
		try {
			JmxUtils.processServer(this.server, this.conn);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
