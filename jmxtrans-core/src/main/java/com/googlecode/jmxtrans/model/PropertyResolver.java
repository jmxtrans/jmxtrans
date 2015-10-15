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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.transformValues;

/***
 *
 * Property Resolver
 *
 * @author henri
 *
 */
public class PropertyResolver {

	private static PropertyResolverFunc RESOLVE_PROPERTIES = new PropertyResolverFunc();

	private static ObjectPropertyResolverFunc RESOLVE_OBJECT_PROPERTIES = new ObjectPropertyResolverFunc();

	/**
	 * Resolve a property from System Properties (aka ${key}) key:defval is
	 * supported and if key not found on SysProps, defval will be returned
	 *
	 * @param s
	 * @return resolved string or null if not found in System Properties and no
	 *         defval
	 */
	private static String resolveString(String s) {

		int pos = s.indexOf(":", 0);

		if (pos == -1)
			return (System.getProperty(s));

		String key = s.substring(0, pos);
		String defval = s.substring(pos + 1);

		String val = System.getProperty(key);

		if (val != null)
			return val;
		else
			return defval;
	}

	/**
	 * Parse a String and replace vars a la ant (${key} from System Properties
	 * Support complex Strings like :
	 *
	 * "${myhost}" "${myhost:w2}" "${mybean:defbean}.${mybean2:defbean2}"
	 *
	 * @param s
	 * @return resolved String
	 */
	public static String resolveProps(@Nullable String s) {
		if (s == null) {
			return null;
		}

		int ipos = 0;
		int pos = s.indexOf("${", ipos);

		if (pos == -1)
			return s;

		StringBuilder sb = new StringBuilder();

		while (ipos < s.length()) {
			pos = s.indexOf("${", ipos);

			if (pos < 0) {
				sb.append(s.substring(ipos));
				break;
			}

			if (pos != ipos)
				sb.append(s.substring(ipos, pos));

			int end = s.indexOf('}', pos);

			if (end < 0)
				break;

			int start = pos + 2;
			pos = end + 1;

			String key = s.substring(start, end);
			String val = resolveString(key);

			if (val != null)
				sb.append(val);
			else
				sb.append("${").append(key).append("}");

			ipos = end + 1;
		}

		return (sb.toString());
	}

	/**
	 * Parse Map and resolve Strings value with resolveProps
	 */
	@CheckReturnValue
	public static ImmutableMap<String, Object> resolveMap(@Nonnull Map<String, Object> map) {
		return copyOf(transformValues(map, RESOLVE_OBJECT_PROPERTIES));
	}

	/**
	 * Parse List and resolve Strings value with resolveProps
	 */
	@CheckReturnValue
	public static ImmutableList<String> resolveList(@Nonnull List<String> list) {
		return from(list)
				.transform(RESOLVE_PROPERTIES)
				.toList();
	}

	private static class PropertyResolverFunc implements Function<String, String> {
		@Nullable
		@Override
		public String apply(@Nullable String input) {
			return resolveProps(input);
		}
	}

	private static class ObjectPropertyResolverFunc implements Function<Object, Object> {
		@Nullable
		@Override
		public Object apply(@Nullable Object input) {
			if (input instanceof String) {
				return resolveProps((String) input);
			}
			return input;
		}
	}
}
