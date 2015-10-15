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
