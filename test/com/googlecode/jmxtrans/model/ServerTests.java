package com.googlecode.jmxtrans.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author lanyonm
 */
public class ServerTests {

	@Test
	public void testGetUrl() {
		// test with host and port
		Server server = new Server("mysys.mydomain", "8004");
		assertEquals("should be 'service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi'", "service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi", server.getUrl());
		// test with url
		server = new Server();
		server.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004");
		assertEquals("should be 'service:jmx:remoting-jmx://mysys.mydomain:8004'", "service:jmx:remoting-jmx://mysys.mydomain:8004", server.getUrl());

		server = new Server();
		server.setUrl("service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi");
		assertEquals("shold be 'service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi'", "service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi", server.getUrl());
	}

	@Test
	public void testGetHostAndPortFromUrl() {
		Server server = new Server();
		server.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004");
		assertEquals("server host should be 'mysys.mydomain'", "mysys.mydomain", server.getHost());
		assertEquals("server port should be '8004'", "8004", server.getPort());
		// test with a different url
		server = new Server();
		server.setUrl("service:jmx:rmi:///jndi/rmi://mysys.mydomain:8004/jmxrmi");
		assertEquals("server host should be 'mysys.mydomain'", "mysys.mydomain", server.getHost());
		assertEquals("server port should be '8004'", "8004", server.getPort());
	}

	@Test
	public void testEquals() {
		Server s1 = new Server();
		s1.setAlias("alias");
		s1.setHost("host");
		s1.setPort("8008");
		s1.setCronExpression("cron");
		s1.setNumQueryThreads(Integer.valueOf(123));
		s1.setPassword("pass");
		s1.setUsername("user");

		Server s2 = new Server();
		s2.setAlias("alias");
		s2.setHost("host");
		s2.setPort("8008");
		s2.setCronExpression("cron");
		s2.setNumQueryThreads(Integer.valueOf(123));
		s2.setPassword("pass");
		s2.setUsername("user");

		Server s3 = new Server();
		s3.setAlias("alias");
		s3.setHost("host3");
		s3.setPort("8008");
		s3.setCronExpression("cron");
		s3.setNumQueryThreads(Integer.valueOf(123));
		s3.setPassword("pass");
		s3.setUsername("user");

		assertFalse(s1.equals(null));
		assertEquals(s1, s1);
		assertFalse(s1.equals("hi"));
		assertEquals(s1, s2);
		assertTrue(s1.equals(s2));
		assertNotSame(s1, s3);
	}

	@Test
	public void testHashCode() {
		Server s1 = new Server();
		s1.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004");
		Server s2 = new Server();
		s2.setUrl("service:jmx:remoting-jmx://mysys.mydomain:8004");
		Server s3 = new Server();
		s3.setUrl("service:jmx:remoting-jmx://mysys3.mydomain:8004");
		assertEquals(s1.hashCode(), s2.hashCode());
		assertFalse(s1.hashCode() == s3.hashCode());
	}
}
