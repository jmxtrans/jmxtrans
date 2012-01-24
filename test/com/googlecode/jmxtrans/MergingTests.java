package com.googlecode.jmxtrans;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;

public class MergingTests {

	@Test
	public void testMerge() throws Exception {
		
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

		// different than q1 and q2
		Query q3 = new Query();
		q3.addAttr("foo");
		q3.addAttr("bar");
		q3.addKey("key1");
		q3.addKey("key2");
		q3.setObj("obj3");
		q3.setResultAlias("alias");

		Server s1 = new Server();
		s1.setAlias("alias");
		s1.setHost("host");
		s1.setCronExpression("cron");
		s1.setNumQueryThreads(Integer.valueOf(123));
		s1.setPassword("pass");
		s1.setUsername("user");
		s1.addQuery(q1);
		s1.addQuery(q2);
		
		// same as s1
		Server s2 = new Server();
		s2.setAlias("alias");
		s2.setHost("host");
		s2.setCronExpression("cron");
		s2.setNumQueryThreads(Integer.valueOf(123));
		s2.setPassword("pass");
		s2.setUsername("user");
		s2.addQuery(q1);
		s2.addQuery(q2);

		Server s3 = new Server();
		s3.setAlias("alias");
		s3.setHost("host3");
		s3.setCronExpression("cron");
		s3.setNumQueryThreads(Integer.valueOf(123));
		s3.setPassword("pass");
		s3.setUsername("user");
		s3.addQuery(q1);
		s3.addQuery(q2);
		s3.addQuery(q3);

		List<Server> existing = new ArrayList<Server>();
		existing.add(s1);

		List<Server> adding = new ArrayList<Server>();

		adding.add(s2);
		JmxUtils.mergeServerLists(existing, adding);

		// should only have one server with 1 query since we just added the same server and same query.
		Assert.assertTrue(existing.size() == 1);
		Assert.assertTrue(existing.get(0).getQueries().size() == 1);

		for (Server server : existing) {
			System.out.println(server);
		}

		adding.add(s3);
		JmxUtils.mergeServerLists(existing, adding);

		Assert.assertTrue(existing.size() == 2);
		Assert.assertTrue(existing.get(0).getQueries().size() == 1); // q1 and q2 are equal
		Assert.assertTrue(existing.get(1).getQueries().size() == 2); // q1 and q2 are equal, q3 is different

		s2.addQuery(q3);
		JmxUtils.mergeServerLists(existing, adding);

		Assert.assertTrue(existing.size() == 2);
		Assert.assertTrue(existing.get(0).getQueries().size() == 2); // q1 and q2 are equal, q3 is different
		Assert.assertTrue(existing.get(1).getQueries().size() == 2); // q1 and q2 are equal, q3 is different
	}
}

