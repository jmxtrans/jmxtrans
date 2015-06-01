package com.googlecode.jmxtrans.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * @author lanyonm
 */
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
		assertNotSame(s1, s3);
	}

	@Test
	public void testServerVariableHandling() {
		try{
			// we add some variables to the System.properties list 
		
			String alias = "somealias";
			String port = "1234";
			String host = "localhost.local";
			String username = "acme";
			String password = "password";
			String url = "service:jmx:remoting-jms://amce.local:1234";
			
			System.setProperty("myalias", alias);
			System.setProperty("myport", port);
			System.setProperty("myhost", host);
			System.setProperty("myusername",username);
			System.setProperty("mypassword", password);
			System.setProperty("myurl", url);
			
			Server serverFromSystemProperties = Server.builder()
						.setAlias("${myalias}")
						.setPort("${myport}")
						.setHost("${myhost}")
						.setUsername("${myusername}")
						.setPassword("${mypassword}")
						.setUrl("${myurl}")
						.build();
			Server serverFromDirectParameters = Server.builder()
						.setAlias(alias)
						.setPort(port)
						.setHost(host)
						.setUsername(username)
						.setPassword(password)
						.setUrl(url)
						.build();
			assertEquals(serverFromSystemProperties.hashCode(), serverFromDirectParameters.hashCode());
			
		}finally{
			System.clearProperty("myalias");
			System.clearProperty("myport");
			System.clearProperty("myhost");
			System.clearProperty("myusername");
			System.clearProperty("myusername");
			System.clearProperty("myurl");
		}
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
}
