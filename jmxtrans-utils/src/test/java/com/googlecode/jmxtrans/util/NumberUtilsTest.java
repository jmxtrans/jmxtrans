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
package com.googlecode.jmxtrans.util;

import org.junit.Assert;
import org.junit.Test;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.lang.Boolean.FALSE;

public class NumberUtilsTest {
	@Test
	public void testIsNumeric() {
		Assert.assertFalse(isNumeric(null));
		Assert.assertTrue(isNumeric("")); // this is "true" for historical
		// reasons
		Assert.assertFalse(isNumeric("  "));
		Assert.assertTrue(isNumeric("123"));
		Assert.assertFalse(isNumeric("12 3"));
		Assert.assertFalse(isNumeric("ab2c"));
		Assert.assertFalse(isNumeric("12-3"));
		Assert.assertTrue(isNumeric("12.3"));
		Assert.assertFalse(isNumeric("12.3.3.3"));
		Assert.assertTrue(isNumeric(".2"));
		Assert.assertFalse(isNumeric("."));
		Assert.assertFalse(isNumeric("3."));
		Assert.assertTrue(isNumeric(1L));
		Assert.assertTrue(isNumeric(2));
		Assert.assertTrue(isNumeric((Object) "3.2"));
		Assert.assertFalse(isNumeric((Object) "abc"));
		Assert.assertFalse(isNumeric(FALSE));
	}
}
