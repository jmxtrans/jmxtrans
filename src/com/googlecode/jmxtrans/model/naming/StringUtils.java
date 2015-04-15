package com.googlecode.jmxtrans.model.naming;

import java.util.regex.Pattern;

public final class StringUtils {
	private static final Pattern DOT_SLASH_UNDERSCORE_PAT = Pattern.compile("[./]");
	private static final Pattern SLASH_UNDERSCORE_PAT = Pattern.compile("/", Pattern.LITERAL);
	private static final Pattern SPACE_PAT = Pattern.compile("[ \"']+");

	private StringUtils() {}

	/**
	 * Replaces all . and / with _ and removes all spaces and double/single quotes.
	 *
	 * @param name the name
	 * @return the string
	 */
	public static String cleanupStr(String name) {
		return cleanupStr(name, false);
	}

	/**
	 * Replaces all . and / with _ and removes all spaces and double/single quotes.
	 *
	 * @param name            the name
	 * @param allowDottedKeys whether we remove the dots or not.
	 * @return
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
		return clean;
	}

	/**
	 * Checks if a String is whitespace, empty (""), equals empty String ("\"\"") or null.
	 *
	 * <pre>
	 * StringUtils.isBlank(null)      = true
	 * StringUtils.isBlank("")        = true
	 * StringUtils.isBlank(" ")       = true
	 * StringUtils.isBlank("\"\"")    = true
	 * StringUtils.isBlank("bob")     = false
	 * StringUtils.isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param name  the String to check, may be null
	 * @return <code>true</code> if the String is null, empty, equals the empty string or whitespace
	 */
	public static boolean isBlank(String name) {
		return org.apache.commons.lang.StringUtils.isBlank(name) || "\"\"".equals(name);
	}
}
