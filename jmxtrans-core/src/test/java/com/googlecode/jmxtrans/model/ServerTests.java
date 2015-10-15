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

import org.junit.Test;

import static org.junit.Assert.*;

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
	public void testServerVariableHandling() {
		try{
			// we add some variables to the System.properties list 
		
			String alias = "somealias";
			String pid = "123";
			String port = "1234";
			String host = "localhost.local";
			String username = "acme";
			String password = "password";
			String url = "service:jmx:remoting-jms://amce.local:1234";
			
			System.setProperty("myalias", alias);
			System.setProperty("mypid", pid);
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

			Server serverPid = Server.builder()
				.setPid("${mypid}")
				.build();

			assertEquals("123", serverPid.getPid());
			
		}finally{
			System.clearProperty("myalias");
			System.clearProperty("mypid");
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

		assertEquals("Server [pid=123, cronExpression=cron, numQueryThreads=2]", s1.toString());
		assertEquals(
			"Server [host=mydomain, port=1234, url=service:jmx:remoting-jmx://mysys.mydomain:8004, cronExpression=cron, numQueryThreads=2]",
			s2.toString());
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
}
