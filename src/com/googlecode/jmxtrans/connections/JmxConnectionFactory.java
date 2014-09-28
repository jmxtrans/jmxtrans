package com.googlecode.jmxtrans.connections;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.IOException;

/**
 * Allows us to pool connections to remote jmx servers.
 */
public class JmxConnectionFactory extends BaseKeyedPoolableObjectFactory<JMXConnectionParams, JMXConnector> {

	/**
	 * Creates the connection.
	 */
	@Override
	public JMXConnector makeObject(JMXConnectionParams params) throws Exception {
		return JMXConnectorFactory.connect(params.getUrl(), params.getEnvironment());
	}

	/**
	 * Closes the connection.
	 */
	@Override
	public void destroyObject(JMXConnectionParams params, JMXConnector connector) throws Exception {
		connector.close();
	}

	/**
	 * Validates that the connection is good.
	 */
	@Override
	public boolean validateObject(JMXConnectionParams params, JMXConnector connector) {
		boolean result = false;
		try {
			connector.getConnectionId();
			connector.getMBeanServerConnection().getMBeanCount();
			result = true;
		} catch (IOException ex) {
			// ignored
		}
		return result;
	}
}
