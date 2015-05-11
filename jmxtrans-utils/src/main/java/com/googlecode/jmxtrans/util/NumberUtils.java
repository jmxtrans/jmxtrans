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
