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
package com.googlecode.jmxtrans.util;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

public final class NumberUtils {

	private NumberUtils() {}

	/**
	 * Useful for figuring out if an Object is a number.
	 */
	public static boolean isNumeric(Object value) {
		if (value == null) return false;
		if (value instanceof Number) return true;
		if (value instanceof String) {
			String stringValue = (String) value;
			if (isNullOrEmpty(stringValue)) return true;
			return isNumber(stringValue);
		}
		return false;
	}

	public static boolean isValidNumber(Object value) {
		if (!(value instanceof Number)) {
			return false;
		}

		Number number = (Number) value;

		if (number instanceof Double) {
			if (Double.isNaN(number.doubleValue()) || Double.isInfinite(number.doubleValue())) {
				return false;
			}
		}

		if (number instanceof Float) {
			if (Float.isNaN(number.floatValue()) || Float.isInfinite(number.floatValue())) {
				return false;
			}
		}

		return true;
	}

}
