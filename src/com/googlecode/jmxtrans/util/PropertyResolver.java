package com.googlecode.jmxtrans.util;

import java.util.List;
import java.util.Map;

/***
 * 
 * Property Resolver
 * 
 * @author henri
 * 
 */
public class PropertyResolver {

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
	public static String resolveProps(String s) {

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
				sb.append("${" + key + "}");

			ipos = end + 1;
		}

		return (sb.toString());
	}

	/**
	 * Parse Map and resolve Strings value with resolveProps
	 */
	public static void resolveMap(Map<String, Object> map) {

		for (String key : map.keySet()) {
			Object val = map.get(key);

			if (val instanceof String)
				map.put(key, resolveProps((String) val));
		}
	}

	/**
	 * Parse List and resolve Strings value with resolveProps
	 */
	public static void resolveList(List<String> list) {

		for (int i = 0; i < list.size(); i++) {
			String val = list.get(i);
			list.set(i, resolveProps(val));
		}
	}
}
