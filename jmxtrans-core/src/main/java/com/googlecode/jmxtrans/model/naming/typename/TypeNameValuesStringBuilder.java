package com.googlecode.jmxtrans.model.naming.typename;

import java.util.List;
import java.util.Map;

public class TypeNameValuesStringBuilder {

	public static final String DEFAULT_SEPARATOR = "_";
	private static final TypeNameValuesStringBuilder defaultBuilder = new TypeNameValuesStringBuilder();
	private String separator;

	public TypeNameValuesStringBuilder() {
		this(DEFAULT_SEPARATOR);
	}

	public TypeNameValuesStringBuilder(String separator) {
		this.separator = separator;
	}

	public String build(List<String> typeNames, String typeNameStr) {
		return doBuild(typeNames, typeNameStr);
	}

	public static TypeNameValuesStringBuilder getDefaultBuilder() {
		return defaultBuilder;
	}

	protected final String doBuild(List<String> typeNames, String typeNameStr) {
		if ((typeNames == null) || (typeNames.size() == 0)) {
			return null;
		}
		Map<String, String> typeNameValueMap = TypeNameValue.extractMap(typeNameStr);
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

}
