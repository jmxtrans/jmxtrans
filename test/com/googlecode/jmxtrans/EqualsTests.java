package com.googlecode.jmxtrans;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

public class EqualsTests {

	@Test
	public void testServer() {
		Server s1 = new Server();
		s1.setAlias("alias");
		s1.setHost("host");
		s1.setCronExpression("cron");
		s1.setNumQueryThreads(Integer.valueOf(123));
		s1.setPassword("pass");
		s1.setUsername("user");

		Server s2 = new Server();
		s2.setAlias("alias");
		s2.setHost("host");
		s2.setCronExpression("cron");
		s2.setNumQueryThreads(Integer.valueOf(123));
		s2.setPassword("pass");
		s2.setUsername("user");

		Server s3 = new Server();
		s3.setAlias("alias");
		s3.setHost("host3");
		s3.setCronExpression("cron");
		s3.setNumQueryThreads(Integer.valueOf(123));
		s3.setPassword("pass");
		s3.setUsername("user");

		Assert.assertEquals(s1, s2);
		Assert.assertTrue(s1.equals(s2));
		Assert.assertNotSame(s1, s3);
	}

	public void testQuery() {
		Query q1 = new Query();
		q1.addAttr("foo");
		q1.addAttr("bar");
		q1.addKey("key1");
		q1.addKey("key2");
		q1.setObj("obj");
		q1.setResultAlias("alias");

		// same as q1
		Query q2 = new Query();
		q2.addAttr("foo");
		q2.addAttr("bar");
		q2.addKey("key1");
		q2.addKey("key2");
		q2.setObj("obj");
		q2.setResultAlias("alias");

		// different
		Query q3 = new Query();
		q3.addAttr("foo");
		q3.addAttr("bar");
		q3.addKey("key1");
		q3.addKey("key2");
		q3.setObj("obj3");
		q3.setResultAlias("alias");

		Assert.assertEquals(q1, q2);
		Assert.assertTrue(q1.equals(q2));
		Assert.assertFalse(q1.equals(q3));

		q1.setResultAlias(null);
		q2.setResultAlias(null);

		Assert.assertEquals(q1, q2);
		Assert.assertTrue(q1.equals(q2));
	}

	public void testQuery2() {
        Query q1 = new Query();
        q1.addAttr("foo");
        q1.addAttr("bar");
        q1.addKey("key1");
        q1.addKey("key2");
        q1.setObj("*");
        q1.setResultAlias("alias");

        // not same as q1
        Query q2 = new Query();
        q2.addAttr("foo");
        q2.addAttr("bar");
        q2.addKey("key1");
        q2.addKey("key2");
        q2.setObj("obj");
        q2.setResultAlias("alias");

        Assert.assertFalse(q1.equals(q2));
	}
}

