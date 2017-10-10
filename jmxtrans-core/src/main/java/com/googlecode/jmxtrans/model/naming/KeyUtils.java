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
package com.googlecode.jmxtrans.model.naming;

import com.google.common.base.Joiner;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;

import java.util.List;

public final class KeyUtils {

	private KeyUtils() {}
	/**
	 * Gets the key string.
	 *
	 *
	 * @param server
	 * @param query      the query
	 * @param result     the result
	 * @param typeNames  the type names
	 * @param rootPrefix the root prefix
	 * @return the key string
	 */
	public static String getKeyString(Server server, Query query, Result result, List<String> typeNames, String rootPrefix) {
		StringBuilder sb = new StringBuilder();
		addRootPrefix(rootPrefix, sb);
		addAlias(server, sb);
		sb.append(".");
		addMBeanIdentifier(query, result, sb);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyString(query, result, sb);
		return sb.toString();
	}

	/**
	 * Gets the key string, without rootPrefix nor Alias
	 *
	 * @param query     the query
	 * @param result    the result
	 * @param typeNames the type names
	 * @return the key string
	 */
	public static String getKeyString(Query query, Result result, List<String> typeNames) {
		StringBuilder sb = new StringBuilder();
		addMBeanIdentifier(query, result, sb);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyString(query, result, sb);
		return sb.toString();
	}

	/**
	 * Gets the key string, without rootPrefix or Alias
	 *
	 * @param query     the query
	 * @param result    the result
	 * @param typeNames the type names
	 * @return the key string
	 */
	public static String getPrefixedKeyString(Query query, Result result, List<String> typeNames) {
		StringBuilder sb = new StringBuilder();
		addTypeName(query, result, typeNames, sb);
		addKeyString(query, result, sb);
		return sb.toString();
	}

	private static void addRootPrefix(String rootPrefix, StringBuilder sb) {
		if (rootPrefix != null) {
			sb.append(rootPrefix);
			sb.append(".");
		}
	}

	private static void addAlias(Server server, StringBuilder sb) {
		String alias;
		if (server.getAlias() != null) {
			alias = server.getAlias();
		} else {
			alias = server.getHost() + "_" + server.getPort();
			alias = StringUtils.cleanupStr(alias);
		}
		sb.append(alias);
	}

	/**
	 * Adds a key to the StringBuilder
	 *
	 * It uses in order of preference:
	 *
	 * 1. resultAlias if that was specified as part of the query
	 * 2. The domain portion of the ObjectName in the query if useObjDomainAsKey is set to true
	 * 3. else, the Class Name of the MBean. I.e. ClassName will be used by default if the
	 * user doesn't specify anything special
	 * @param query
	 * @param result
	 * @param sb
	 */
	private static void addMBeanIdentifier(Query query, Result result, StringBuilder sb) {
		if (result.getKeyAlias() != null) {
			sb.append(result.getKeyAlias());
		} else if (query.isUseObjDomainAsKey()) {
			sb.append(StringUtils.cleanupStr(result.getObjDomain(), query.isAllowDottedKeys()));
		} else {
			sb.append(StringUtils.cleanupStr(result.getClassName()));
		}
	}

	private static void addTypeName(Query query, Result result, List<String> typeNames, StringBuilder sb) {
		String typeName = StringUtils.cleanupStr(query.makeTypeNameValueString(typeNames, result.getTypeName()), query.isAllowDottedKeys());
		if (typeName != null && typeName.length() > 0) {
			sb.append(typeName);
			sb.append(".");
		}
	}

	private static void addKeyString(Query query, Result result, StringBuilder sb) {
		String keyStr = getValueKey(result);
		sb.append(StringUtils.cleanupStr(keyStr, query.isAllowDottedKeys()));
	}

	public static String getValueKey(Result result) {
		if (result.getValuePath().isEmpty()) {
			return result.getAttributeName();
		}
		return result.getAttributeName() + "." + getValuePathString(result);
	}

	public static String getValuePathString(Result result) {
		return Joiner.on('.').join(result.getValuePath());
	}

}
