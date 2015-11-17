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

import java.util.regex.Pattern;

public final class StringUtils {
	private static final Pattern DOT_SLASH_UNDERSCORE_PAT = Pattern.compile("[./]");
	private static final Pattern SLASH_UNDERSCORE_PAT = Pattern.compile("/", Pattern.LITERAL);
	private static final Pattern SPACE_PAT = Pattern.compile("[ \"']+");

	private StringUtils() {}

	/**
	 * Replaces all . and / with _ and removes all spaces and double/single quotes.
	 */
	public static String cleanupStr(String name) {
		return cleanupStr(name, false);
	}

	/**
	 * Replaces all . and / with _ and removes all spaces and double/single quotes.
	 * Chomps any trailing . or _ character.
	 *
	 * @param allowDottedKeys whether we remove the dots or not.
	 */
	public static String cleanupStr(String name, boolean allowDottedKeys) {
		if (name == null) {
			return null;
		}
		Pattern pattern;
		if (!allowDottedKeys) {
			pattern = DOT_SLASH_UNDERSCORE_PAT;
		} else {
			pattern = SLASH_UNDERSCORE_PAT;
		}
		String clean = pattern.matcher(name).replaceAll("_");
		clean = SPACE_PAT.matcher(clean).replaceAll("");
		clean = org.apache.commons.lang.StringUtils.chomp(clean, ".");
		clean = org.apache.commons.lang.StringUtils.chomp(clean, "_");
		return clean;
	}
}
