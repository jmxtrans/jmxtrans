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
package com.googlecode.jmxtrans.model;

import com.googlecode.jmxtrans.connections.JMXConnection;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.kaching.platform.testing.AllowDNSResolution;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InOrder;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author lanyonm
 */
@Category(RequiresIO.class)
@AllowDNSResolution
public class ServerTests {

	@Test
	public void testGetUrl() {
		// test with host and port
		Server server = Server.builder().setHost("mysys.mydomain").setPort("8004").build();
		assertEquals("should be 'service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi'", "service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi", server.getUrl());
		// test with url
		server = Server.builder()
				.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004")
				.build();
		assertEquals("should be 'service:jmx:remoting-jmx://mysys.mydomain:8004'", "service:jmx:remoting-jmx://mysys.mydomain:8004", server.getUrl());

		server = Server.builder()
				.setUrl("service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi")
				.build();
		assertEquals("shold be 'service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi'", "service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi", server.getUrl());
	}

	@Test
	public void testGetHostAndPortFromUrl() {
		Server server = Server.builder()
				.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004")
				.build();
		assertEquals("server host should be 'mysys.mydomain'", "mysys.mydomain", server.getHost());
		assertEquals("server port should be '8004'", "8004", server.getPort());
		// test with a different url
		server = Server.builder()
				.setUrl("service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi")
				.build();
		assertEquals("server host should be 'mysys.mydomain'", "mysys.mydomain", server.getHost());
		assertEquals("server port should be '8004'", "8004", server.getPort());
	}

	@Test
	public void testEquals() {
		Server s1 = Server.builder()
				.setAlias("alias")
				.setHost("host")
				.setPort("8008")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.build();

		Server s2 = Server.builder()
				.setAlias("alias")
				.setHost("host")
				.setPort("8008")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.build();

		Server s3 = Server.builder()
				.setAlias("alias")
				.setHost("host3")
				.setPort("8008")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.build();

		assertFalse(s1.equals(null));
		assertEquals(s1, s1);
		assertFalse(s1.equals("hi"));
		assertEquals(s1, s2);
		assertTrue(s1.equals(s2));
		assertNotEquals(s1, s3);
	}

	@Test
	public void testEquals_forPid() {
		Server s1 = Server.builder().setPid("1").build();
		Server s2 = Server.builder().setPid("2").build();
		Server s3 = Server.builder(s1).build();

		assertEquals(s1, s3);
		assertNotEquals(s1, s2);
	}

	@Test
	public void testHashCode() {
		Server s1 = Server.builder()
				.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004")
				.build();
		Server s2 = Server.builder()
				.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004")
				.build();
		Server s3 = Server.builder()
				.setUrl("service:jmx:remoting-jmx://mysys3.mydomain:8004")
				.build();
		assertEquals(s1.hashCode(), s2.hashCode());
		assertFalse(s1.hashCode() == s3.hashCode());
	}

	@Test
	public void testToString() {
		Server s1 = Server.builder()
				.setPid("123")
				.setCronExpression("cron")
				.setNumQueryThreads(2)
				.build();

		Server s2 = Server.builder()
			.setHost("mydomain")
			.setPort("1234")
			.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004")
			.setCronExpression("cron")
			.setNumQueryThreads(2)
			.build();

		assertThat(s1.toString())
				.contains("pid=123")
				.contains("cronExpression=cron")
				.contains("numQueryThreads=2");
		assertThat(s2.toString())
				.contains("host=mydomain")
				.contains("port=1234")
				.contains("url=service:jmx:remoting-jmx://mysys.mydomain:8004")
				.contains("cronExpression=cron")
				.contains("numQueryThreads=2");
	}

	@Test
	public void testIntegrity() {
		try {
			Server.builder().setPid("123").setUrl("aaa").build();
			fail("Pid and Url should not be allowed at the same time");
		}
		catch(IllegalArgumentException e) {}

		try {
			Server.builder().build();
			fail("No Pid or Url can't work");
		}
		catch(IllegalArgumentException e) {}
	}

	@Test
	public void testConnectionRepoolingOk() throws Exception {
		@SuppressWarnings("unchecked")
		GenericKeyedObjectPool<Server, JMXConnection> pool = mock(GenericKeyedObjectPool.class);

		Server server = Server.builder()
				.setHost("host.example.net")
				.setPort("4321")
				.setLocal(true)
				.setPool(pool)
				.build();

		MBeanServerConnection mBeanConn = mock(MBeanServerConnection.class);

		JMXConnection conn = mock(JMXConnection.class);
		when(conn.getMBeanServerConnection()).thenReturn(mBeanConn);

		when(pool.borrowObject(server)).thenReturn(conn);

		Query query = mock(Query.class);
		Iterable<ObjectName> objectNames = Lists.emptyList();
		when(query.queryNames(mBeanConn)).thenReturn(objectNames);
		server.execute(query);

		verify(pool, never()).invalidateObject(server, conn);

		InOrder orderVerifier = inOrder(pool);
		orderVerifier.verify(pool).borrowObject(server);
		orderVerifier.verify(pool).returnObject(server, conn);
	}

	@Test
	public void testConnectionRepoolingSkippedOnError() throws Exception {
		@SuppressWarnings("unchecked")
		GenericKeyedObjectPool<Server, JMXConnection> pool = mock(GenericKeyedObjectPool.class);

		Server server = Server.builder()
				.setHost("host.example.net")
				.setPort("4321")
				.setLocal(true)
				.setPool(pool)
				.build();

		MBeanServerConnection mBeanConn = mock(MBeanServerConnection.class);

		JMXConnection conn = mock(JMXConnection.class);
		when(conn.getMBeanServerConnection()).thenReturn(mBeanConn);

		when(pool.borrowObject(server)).thenReturn(conn);

		Query query = mock(Query.class);
		IOException e = mock(IOException.class);
		when(query.queryNames(mBeanConn)).thenThrow(e);

		try {
			server.execute(query);
			fail("No exception got throws");
		} catch (IOException e2) {
			if (e != e2) {
				fail("Wrong exception thrown (" + e + " instead of mock");
			}
		}

		verify(pool, never()).returnObject(server, conn);;

		InOrder orderVerifier = inOrder(pool);
		orderVerifier.verify(pool).borrowObject(server);
		orderVerifier.verify(pool).invalidateObject(server, conn);
	}
}
