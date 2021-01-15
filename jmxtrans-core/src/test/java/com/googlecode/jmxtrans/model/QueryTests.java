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

import org.junit.Test;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryTests {
	@Test
	public void testBuilderTypeNamesListFooBar() {
		List<String> expected = newArrayList("foo", "bar");

		Query query = Query.builder()
				.setObj("obj:key=val")
				.setTypeNames(expected)
				.build();

		List<String> actual = newArrayList(query.getTypeNames());
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testBuilderTypeNamesListBarFoo() {
		List<String> expected = newArrayList("bar", "foo");

		Query query = Query.builder()
				.setObj("obj:key=val")
				.setTypeNames(expected)
				.build();

		List<String> actual = newArrayList(query.getTypeNames());
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testBuilderTypeNamesSetFooBar() {
		List<String> expected = newArrayList("foo", "bar");

		Query query = Query.builder()
				.setObj("obj:key=val")
				.setTypeNames(newLinkedHashSet(expected))
				.build();

		List<String> actual = newArrayList(query.getTypeNames());
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testBuilderTypeNamesSetBarFoo() {
		List<String> expected = newArrayList("bar", "foo");

		Query query = Query.builder()
				.setObj("obj:key=val")
				.setTypeNames(newLinkedHashSet(expected))
				.build();

		List<String> actual = newArrayList(query.getTypeNames());
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testBuilderTypeNameValueStringFooBar() {
		List<String> typeNames = copyOf(newArrayList("foo", "bar"));
		String typeNameStr = "foo=FOO,bar=BAR,baz=BAZ";
		Query query = Query.builder()
				.setObj("obj:" + typeNameStr)
				.setTypeNames(typeNames)
				.build();

		String actual = query.makeTypeNameValueString(typeNames, typeNameStr);

		assertThat(actual).isEqualTo("FOO_BAR");
	}

	@Test
	public void testBuilderTypeNameValueStringBarFoo() {
		List<String> typeNames = copyOf(newArrayList("bar", "foo"));
		String typeNameStr = "foo=FOO,bar=BAR,baz=BAZ";
		Query query = Query.builder()
				.setObj("obj:" + typeNameStr)
				.setTypeNames(typeNames)
				.build();

		String actual = query.makeTypeNameValueString(typeNames, typeNameStr);

		assertThat(actual).isEqualTo("BAR_FOO");
	}
}
