package com.googlecode.jmxtrans;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.jmxtrans.model.Query;

public class EqualsTests {

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
