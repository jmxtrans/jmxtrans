/**
 * The MIT License
 * Copyright © 2018 JmxTrans team
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
package com.googlecode.jmxtrans.model.naming;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQueryWithResultAlias;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.SERVER_ALIAS;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServerBuilder;
import static com.googlecode.jmxtrans.model.ServerFixtures.serverWithAliasAndNoQuery;
import static org.junit.Assert.assertEquals;

public class KeyUtilsTest {

	@Test
	public void testKeyString() {
		assertEquals("rootPrefix." + SERVER_ALIAS + ".MemoryAlias.ObjectPendingFinalizationCount",
				KeyUtils.getKeyString(
						serverWithAliasAndNoQuery(),
						dummyQueryWithResultAlias(),
						numericResult(),
						ImmutableList.of("typeName"),
						"rootPrefix"));
		assertEquals("rootPrefix.ObjectPendingFinalizationCount",
				KeyUtils.getKeyString(
						dummyServerBuilder().setAlias("").build(),
						dummyQuery(),
						numericResult("", 10),
						ImmutableList.of("typeName"),
						"rootPrefix"));
		assertEquals("MemoryAlias.ObjectPendingFinalizationCount",
				KeyUtils.getKeyString(
						dummyServerBuilder().setAlias("").build(),
						dummyQuery(),
						numericResult(10),
						ImmutableList.of("typeName"),
						""));
		assertEquals(SERVER_ALIAS + ".ObjectPendingFinalizationCount",
				KeyUtils.getKeyString(
						dummyServerBuilder().setAlias(SERVER_ALIAS).build(),
						dummyQuery(),
						numericResult("", 10),
						ImmutableList.of("typeName"),
						""));
		assertEquals("ObjectPendingFinalizationCount",
				KeyUtils.getKeyString(
						dummyServerBuilder().setAlias("").build(),
						dummyQuery(),
						numericResult("", 10),
						ImmutableList.of("typeName"),
						""));
	}
}
