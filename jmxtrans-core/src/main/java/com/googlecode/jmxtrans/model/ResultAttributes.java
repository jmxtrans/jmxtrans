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
package com.googlecode.jmxtrans.model;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * Enumerates the attributes of {@link Result}
 *
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 */
public final class ResultAttributes {
	private ResultAttributes() {
	}

	public static final ResultAttribute TYPE_NAME = new ResultAttribute("typeName") {
		@Override
		public String get(Result result) {
			return result.getTypeName();
		}
	};

	public static final ResultAttribute OBJ_DOMAIN = new ResultAttribute("objDomain") {
		@Override
		public String get(Result result) {
			return result.getObjDomain();
		}
	};

	public static final ResultAttribute CLASS_NAME = new ResultAttribute("className") {
		@Override
		public String get(Result result) {
			return result.getClassName();
		}
	};

	public static final ResultAttribute ATTRIBUTE_NAME = new ResultAttribute("attributeName") {
		@Override
		public String get(Result result) {
			return result.getAttributeName();
		}
	};

	/**
	 * Implementation of {@link ResultAttribute} to lookup type name properties
	 */
	private static final class TypeNameProperty extends ResultAttribute {
		private static final String PREFIX = "typeName.";
		private final String propertyName;

		private TypeNameProperty(String propertyName) {
			super(PREFIX + propertyName);
			this.propertyName = propertyName;
		}

		@Override
		public String get(Result result) {
			return result.getTypeNameMap().get(propertyName);
		}
	}

	/**
	 * Get the {@link ResultAttributes} value from the attribute name
	 *
	 * @param attributeName <p>The attribute name for the {@link ResultAttribute} allowed values are:</p>
	 *                      <ul>
	 *                      <li>typeName</li>
	 *                      <li>objDomain</li>
	 *                      <li>className</li>
	 *                      <li>attributeName</li>
	 *                      </ul>
	 * @return the {@link ResultAttribute}
	 */
	public static ResultAttribute forName(@Nonnull String attributeName) {
		if (attributeName.startsWith(TypeNameProperty.PREFIX)) {
			return new TypeNameProperty(attributeName.substring(TypeNameProperty.PREFIX.length()));
		}
		String[] split = StringUtils.splitByCharacterTypeCamelCase(attributeName);
		StringBuilder sb = new StringBuilder(split[0].toUpperCase()).append("_").append(split[1].toUpperCase());
		return valueOf(sb.toString());
	}

	/**
	 * Get the {@link ResultAttributes} value for each attribute name.
	 *
	 * @return Set of {@link ResultAttribute}
	 * @see #forName(String)
	 */
	public static ImmutableSet<ResultAttribute> forNames(@Nonnull Collection<String> attributeNames) {
		ImmutableSet.Builder<ResultAttribute> builder = ImmutableSet.<ResultAttribute>builder();
		for (String attributeName : attributeNames) {
			builder.add(forName(attributeName));
		}
		return builder.build();
	}

	/**
	 * Get {@link ResultAttribute}s by its constant name
	 *
	 * @param attributeName <p>The attribute name for the {@link ResultAttribute} allowed values are:</p>
	 *                      <ul>
	 *                      <li>TYPE_NAME</li>
	 *                      <li>OBJ_DOMAIN</li>
	 *                      <li>CLASS_NAME</li>
	 *                      <li>ATTRIBUTE_NAME</li>
	 *                      </ul>
	 */
	public static ResultAttribute valueOf(@Nonnull String attributeName) {
		ResultAttribute value;
		switch (attributeName) {
			case "TYPE_NAME":
				value = TYPE_NAME;
				break;
			case "OBJ_DOMAIN":
				value = OBJ_DOMAIN;
				break;
			case "CLASS_NAME":
				value = CLASS_NAME;
				break;
			case "ATTRIBUTE_NAME":
				value = ATTRIBUTE_NAME;
				break;
			default:
				throw new IllegalArgumentException("Invalid value " + attributeName);
		}
		return value;
	}

	/**
	 * Get known {@link ResultAttribute}s as if it was an enumeration
	 */
	public static List<ResultAttribute> values() {
		return Arrays.asList(TYPE_NAME, OBJ_DOMAIN, CLASS_NAME, ATTRIBUTE_NAME);
	}
}
