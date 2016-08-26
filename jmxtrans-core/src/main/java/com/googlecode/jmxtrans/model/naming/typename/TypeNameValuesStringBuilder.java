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
package com.googlecode.jmxtrans.model.naming.typename;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.chomp;

@Immutable
@EqualsAndHashCode
@ToString
public class TypeNameValuesStringBuilder {

	public static final String DEFAULT_SEPARATOR = "_";
	private static final TypeNameValuesStringBuilder defaultBuilder = new TypeNameValuesStringBuilder();
	private final String separator;

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
		if ((typeNames == null) || (typeNames.isEmpty())) {
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
		return chomp(sb.toString(), separator);
	}

}
