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
package com.googlecode.jmxtrans.model;

import com.googlecode.jmxtrans.connections.JMXConnection;
import com.googlecode.jmxtrans.connections.JMXConnectionProvider;
import com.googlecode.jmxtrans.connections.MBeanServerConnectionFactory;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

public final class ServerFixtures {

	public static final String DEFAULT_HOST = "host.example.net";
	public static final String DEFAULT_PORT = "4321";
	public static final String SERVER_ALIAS = "myAlias";
	public static final String DEFAULT_QUERY = "myQuery:key=val";

	private ServerFixtures() {}

	public static Server createServerWithOneQuery(String host, String port, String queryObject) {
		return getBuilder(host, port, queryObject).build();
	}

	private static Server.Builder getBuilder(String host, String port, String queryObject) {
		return Server.builder()
				.setHost(host)
				.setPort(port)
				.setPool(createPool())
				.addQuery(Query.builder()
					.setObj(queryObject)
					.build());
	}

	public static Server serverWithNoQuery() {
		return Server.builder()
				.setHost(DEFAULT_HOST)
				.setPort(DEFAULT_PORT)
				.setPool(createPool())
				.build();
	}

	public static Server serverWithAliasAndNoQuery() {
		return Server.builder()
				.setHost(DEFAULT_HOST)
				.setPort(DEFAULT_PORT)
				.setAlias(SERVER_ALIAS)
				.setPool(createPool())
				.build();
	}

	public static Server dummyServer() {
		return createServerWithOneQuery(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_QUERY);
	}

	public static Server.Builder dummyServerBuilder() {
		return getBuilder(DEFAULT_HOST, DEFAULT_PORT, "myQuery:key=val");
	}

	public static Server localServer() {
		return Server.builder()
				.setHost(DEFAULT_HOST)
				.setPort(DEFAULT_PORT)
				.setLocal(true)
				.setPool(createPool())
				.build();
	}

	public static KeyedObjectPool<JMXConnectionProvider, JMXConnection> createPool() {
		return new GenericKeyedObjectPool<>(new MBeanServerConnectionFactory());
	}

}
