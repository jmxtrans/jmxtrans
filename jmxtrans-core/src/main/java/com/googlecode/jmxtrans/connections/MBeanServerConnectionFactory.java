/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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

import javax.annotation.Nonnull;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MBeanServerConnectionFactory extends BaseKeyedPoolableObjectFactory<JmxConnectionProvider, JMXConnection> {
	@Nonnull private final ExecutorService executor;

	public MBeanServerConnectionFactory(){
		executor = Executors.newCachedThreadPool();
	}

	@Override
	@Nonnull
	public JMXConnection makeObject(@Nonnull JmxConnectionProvider server) throws IOException {
		if (server.isLocal()) {
			return new JMXConnection(null, server.getLocalMBeanServer());
		} else {
			JMXConnector connection = server.getServerConnection();
			return new JMXConnection(connection, connection.getMBeanServerConnection());
		}
	}

	@Override
	public void destroyObject(@Nonnull JmxConnectionProvider key, @Nonnull final JMXConnection jmxConnection) throws IOException {
		if (!jmxConnection.isAlive()) {
			return;
		}

		jmxConnection.setMarkedAsDestroyed();

		// when you call close it tries to notify server about it
		// so if your server is down you will wait until connection timed out
		// but we want to release thread asap
		// that's why we do it here through ExecutorService
		executor.submit(new Runnable() {
			public void run() {
				jmxConnection.close();
			}
		});
	}

	@Override
	public boolean validateObject(@Nonnull JmxConnectionProvider key, @Nonnull JMXConnection jmxConnection){
		return jmxConnection.isAlive();
	}

}
