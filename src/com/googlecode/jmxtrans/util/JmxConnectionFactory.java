package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.model.Server;
import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import java.io.IOException;

/**
 * Allows us to pool connections to remote jmx servers.
 */
public class JmxConnectionFactory extends BaseKeyedPoolableObjectFactory {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(JmxConnectionFactory.class);

	/** constructor */
	public JmxConnectionFactory() {
	}

	/**
	 * Creates the connection.
	 */
	@Override
	public Object makeObject(Object key) throws Exception {
		Server server = (Server) key;
		return JmxUtils.getServerConnection(server);
	}

	/**
	 * Closes the connection.
	 */
	@Override
	public void destroyObject(Object key, Object obj) throws Exception {
		JMXConnector conn = (JMXConnector) obj;
		conn.close();
	}

	/**
	 * Validates that the connection is good.
	 */
	@Override
	public boolean validateObject(Object key, Object obj) {
		JMXConnector conn = (JMXConnector) obj;
		boolean result = false;
		try {
			conn.getConnectionId();
            conn.getMBeanServerConnection().getMBeanCount();
			result = true;
		} catch (IOException ex) {
			// ignored
		}
		return result;
	}
}
