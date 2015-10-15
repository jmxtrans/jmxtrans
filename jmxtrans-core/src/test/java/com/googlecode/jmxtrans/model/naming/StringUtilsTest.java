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
package com.googlecode.jmxtrans.model.naming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest {

	@Test
	public void testCleanupStr() {
		assertEquals("addfber1241qdw!èé$", StringUtils.cleanupStr("addfber1241qdw!èé$"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd.abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd.abcd."));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr(".abcd_abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd/"));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd_abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd'_abcd'"));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd\"_abcd\""));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/ab cd_abcd"));
		assertEquals(null, StringUtils.cleanupStr(null));
		assertEquals("", StringUtils.cleanupStr(""));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\".\"\""));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\"_\"\""));
	}

	@Test
	public void testCleanupStrDottedKeysKept() {
		assertEquals("addfber1241qdw!èé$", StringUtils.cleanupStr("addfber1241qdw!èé$", true));
		assertEquals("abcd.abcd", StringUtils.cleanupStr("abcd.abcd", true));
		assertEquals("abcd.abcd", StringUtils.cleanupStr("abcd.abcd.", true));
		assertEquals(".abcd_abcd", StringUtils.cleanupStr(".abcd_abcd", true));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd", true));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd/", true));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd_abcd", true));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd'_abcd'", true));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd\"_abcd\"", true));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/ab cd_abcd", true));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\".\"\""));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\"_\"\""));
	}

}
