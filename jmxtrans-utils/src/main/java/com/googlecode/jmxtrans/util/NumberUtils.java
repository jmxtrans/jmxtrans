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

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public final class NumberUtils {
	private static final Pattern IS_NUMERIC_PAT = Pattern.compile("\\d*(?:[.]\\d+)?");

	private NumberUtils() {}

	/**
	 * Useful for figuring out if an Object is a number.
	 */
	public static boolean isNumeric(Object value) {
		return ((value instanceof Number) || ((value instanceof String) && isNumeric((String) value)));
	}

	/**
	 * <p>
	 * Checks if the String contains only unicode digits. A decimal point is a
	 * digit and returns true.
	 * </p>
	 * <p/>
	 * <p>
	 * <code>null</code> will return <code>false</code>. An empty String ("")
	 * will return <code>true</code>.
	 * </p>
	 * <p/>
	 * <pre>
	 * StringUtils.isNumeric(null)   = false
	 * StringUtils.isNumeric("")     = true
	 * StringUtils.isNumeric("  ")   = false
	 * StringUtils.isNumeric("123")  = true
	 * StringUtils.isNumeric("12 3") = false
	 * StringUtils.isNumeric("ab2c") = false
	 * StringUtils.isNumeric("12-3") = false
	 * StringUtils.isNumeric("12.3") = true
	 * </pre>
	 *
	 * @param str the String to check, may be null
	 * @return <code>true</code> if only contains digits, and is non-null
	 * @deprecated There is already a dependency in this project on Apache
	 * common-lang, so you should probably use {@see org.apache.commons.lang.math.NumberUtils}.
	 */
	@Deprecated
	public static boolean isNumeric(String str) {
		if (StringUtils.isEmpty(str)) {
			return str != null; // Null = false, empty = true
		}
		return IS_NUMERIC_PAT.matcher(str).matches();
	}
}
