package com.googlecode.jmxtrans.model.naming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;

import static com.google.common.collect.Maps.newHashMap;

public final class KeyUtils {
	private KeyUtils() {}
	/**
	 * Gets the key string.
	 *
	 *
	 * @param server
	 * @param query      the query
	 * @param result     the result
	 * @param values     the values
	 * @param typeNames  the type names
	 * @param rootPrefix the root prefix
	 * @return the key string
	 */
	public static String getKeyString(Server server, Query query, Result result, Map.Entry<String, Object> values, List<String> typeNames, String rootPrefix, boolean useObjDomain) {
		StringBuilder sb = new StringBuilder();
		addRootPrefix(rootPrefix, sb);
		addAlias(server, sb);
		sb.append(".");
		addKey(result, sb, useObjDomain);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyString(result, values, sb);
		return sb.toString();
	}

	/**
	 * Gets the key string, without rootPrefix nor Alias
	 *
	 * @param query     the query
	 * @param result    the result
	 * @param values    the values
	 * @param typeNames the type names
	 * @return the key string
	 */
	public static String getKeyString(Query query, Result result, Map.Entry<String, Object> values, List<String> typeNames) {
		StringBuilder sb = new StringBuilder();
		addKey(result, sb, false);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyString(result, values, sb);
		return sb.toString();
	}

	/**
	 * Gets the key string, with dot allowed
	 *
	 * @param query     the query
	 * @param result    the result
	 * @param values    the values
	 * @param typeNames the type names
	 * @return the key string
	 */
	public static String getKeyStringWithDottedKeys(Query query, Result result, Map.Entry<String, Object> values, List<String> typeNames) {
		StringBuilder sb = new StringBuilder();
		addKey(result, sb, false);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyStringDotted(result, values, query.isAllowDottedKeys(), sb);
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
	 * 2. The domain portion of the ObjectName in the query if useObjDomain is set to true
	 * 3. else, the Class Name of the MBean. I.e. ClassName will be used by default if the 
	 * user doesn't specify anything special
	 * 
	 * @param result
	 * @param sb
	 * @param useObjectDomain
	 */
	private static void addKey(Result result, StringBuilder sb, boolean useObjectDomain) {
		if (result.getKeyAlias() != null) {
			sb.append(result.getKeyAlias());
		} else if (useObjectDomain) {
			sb.append(StringUtils.cleanupStr(result.getObjDomain(), true));
		} else {
			sb.append(StringUtils.cleanupStr(result.getClassName()));
		}
	}

	private static void addTypeName(Query query, Result result, List<String> typeNames, StringBuilder sb) {
		String typeName = StringUtils.cleanupStr(getConcatedTypeNameValues(query, typeNames, result.getTypeName()), query.isAllowDottedKeys());
		if (typeName != null && typeName.length() > 0) {
			sb.append(typeName);
			sb.append(".");
		}
	}

	private static void addKeyString(Result result, Map.Entry<String, Object> values, StringBuilder sb) {
		String keyStr = computeKey(result, values);
		sb.append(StringUtils.cleanupStr(keyStr));
	}

	private static void addKeyStringDotted(Result result, Map.Entry<String, Object> values, boolean isAllowDottedKeys, StringBuilder sb) {
		String keyStr = computeKey(result, values);
		sb.append(StringUtils.cleanupStr(keyStr, isAllowDottedKeys));
	}

	private static String computeKey(Result result, Map.Entry<String, Object> values) {
		String keyStr;
		if (values.getKey().startsWith(result.getAttributeName())) {
			keyStr = values.getKey();
		} else {
			keyStr = result.getAttributeName() + "." + values.getKey();
		}
		return keyStr;
	}

	/**
	 * Given a typeName string, get the first match from the typeNames setting.
	 * In other words, suppose you have:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the
	 * string
	 *
	 * @param typeNames   the type names
	 * @param typeNameStr the type name str
	 * @return the concated type name values
	 */
	public static String getConcatedTypeNameValues(List<String> typeNames, String typeNameStr) {
		return getConcatedTypeNameValues(typeNames, typeNameStr, getTypeNameValuesSeparator(null));
	}

	/**
	 * Given a typeName string, get the first match from the typeNames setting.
	 * In other words, suppose you have:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the
	 * string
	 *
	 * @param typeNames   the type names
	 * @param typeNameStr the type name str
	 * @param separator
	 * @return the concated type name values
	 */
	public static String getConcatedTypeNameValues(List<String> typeNames, String typeNameStr, String separator) {
		if ((typeNames == null) || (typeNames.size() == 0)) {
			return null;
		}
		Map<String, String> typeNameValueMap = getTypeNameValueMap(typeNameStr);
		StringBuilder sb = new StringBuilder();
		for (String key : typeNames) {
			String result = typeNameValueMap.get(key);
			if (result != null) {
				sb.append(result);
				sb.append(separator);
			}
		}
		return org.apache.commons.lang.StringUtils.chomp(sb.toString(), separator);
	}

	/**
	 * Given a typeName string, create a Map with every key and value in the typeName.
	 * For example:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * Returns a Map with the following key/value pairs (excluding the quotes):
	 * <p/>
	 * "name"  =>  "PS Eden Space"
	 * "type"  =>  "MemoryPool"
	 *
	 * @param typeNameStr the type name str
	 * @return Map<String, String> of type-name-key / value pairs.
	 */
	public static Map<String, String> getTypeNameValueMap(String typeNameStr) {
		if (typeNameStr == null) {
			return Collections.emptyMap();
		}

		Map<String, String> result = newHashMap();
		String[] tokens = typeNameStr.split(",");

		for (String oneToken : tokens) {
			if (oneToken.length() > 0) {
				String[] keyValue = splitTypeNameValue(oneToken);
				result.put(keyValue[0], keyValue[1]);
			}
		}
		return result;
	}

	/**
	 * Given a typeName string, get the first match from the typeNames setting.
	 * In other words, suppose you have:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the
	 * string
	 *
	 * @param query     the query
	 * @param typeNames the type names
	 * @param typeName  the type name
	 * @return the concated type name values
	 */
	public static String getConcatedTypeNameValues(Query query, List<String> typeNames, String typeName) {
		Set<String> queryTypeNames = query.getTypeNames();
		if (queryTypeNames != null && queryTypeNames.size() > 0) {
			List<String> filteredTypeNames = new ArrayList<String>(queryTypeNames);
			for (String name : typeNames) {
				if (!filteredTypeNames.contains(name)) {
					filteredTypeNames.add(name);
				}
			}
			return getConcatedTypeNameValues(filteredTypeNames, typeName, getTypeNameValuesSeparator(query));
		} else {
			return getConcatedTypeNameValues(typeNames, typeName, getTypeNameValuesSeparator(query));
		}
	}

	/**
	 * Given a query, return a separator for type names based on its configuration.
	 * If query is null, return the default separator.
	 *
	 * @param query   the query
	 * @return the separator
	 */
	private static String getTypeNameValuesSeparator(Query query) {
		if (query != null && query.isAllowDottedKeys()) {
			return ".";
		}
		return "_";
	}

	/**
	 * Given a single type-name-key and value from a typename strig (e.g. "type=MemoryPool"), extract the key and
	 * the value and return both.
	 *
	 * @param typeNameToken - the string containing the pair.
	 * @return String[2] where String[0] = the key and String[1] = the value.  If the given string is not in the
	 * format "key=value" then String[0] = the original string and String[1] = "".
	 */
	private static String[] splitTypeNameValue(String typeNameToken) {
		String[] result;
		String[] keys = typeNameToken.split("=", 2);

		if (keys.length == 2) {
			result = keys;
		} else {
			result = new String[2];
			result[0] = keys[0];
			result[1] = "";
		}

		return result;
	}
}
