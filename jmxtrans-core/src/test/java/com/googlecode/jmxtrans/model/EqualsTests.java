package com.googlecode.jmxtrans.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EqualsTests {

	@Test
	public void testQuery() {
		Query q1 = Query.builder()
				.setObj("obj")
				.addKeys("key1", "key2")
				.addAttr("foo", "bar")
				.setResultAlias("alias")
				.build();

		// same as q1
		Query q2 = Query.builder()
				.setObj("obj")
				.addKeys("key1", "key2")
				.addAttr("foo", "bar")
				.setResultAlias("alias")
				.build();

		// different
		Query q3 = Query.builder()
				.setObj("obj3")
				.addKeys("key1", "key2")
				.addAttr("foo", "bar")
				.setResultAlias("alias")
				.build();

		assertThat(q1).isEqualTo(q2);
		assertThat(q1).isNotEqualTo(q3);
	}

	@Test
	public void testQuery2() {
		Query q1 = Query.builder()
				.setObj("*")
				.addKeys("key1", "key2")
				.addAttr("foo", "bar")
				.setResultAlias("alias")
				.build();

		// not same as q1
		Query q2 = Query.builder()
				.setObj("obj")
				.addKeys("key1", "key2")
				.addAttr("foo", "bar")
				.setResultAlias("alias")
				.build();

		assertThat(q1).isNotEqualTo(q2);
	}
}
