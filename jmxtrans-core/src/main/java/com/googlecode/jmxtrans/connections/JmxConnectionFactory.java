/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
