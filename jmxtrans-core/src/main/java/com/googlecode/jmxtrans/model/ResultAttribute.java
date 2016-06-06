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

import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.capitalize;

/**
 * Enumerates the attributes of {@link Result}
 * 
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 *
 */
@ToString
public enum ResultAttribute {

	TYPE_NAME("typeName"), OBJ_DOMAIN("objDomain"), CLASS_NAME("className"), ATTRIBUTE_NAME("attributeName");

	@Nonnull
	private String attributeName;
	@Nonnull
	private String accessorMethod;

	ResultAttribute(String attributeName) {
		this.attributeName = attributeName;
		this.accessorMethod = "get" + capitalize(attributeName);
	}

	/**
	 * Get the {@link ResultAttribute} value from the attribute name
	 * 
	 * @param attributeName
	 *            <p>The attribute name for the {@link ResultAttribute} allowed values are:</p>
	 *            <ul>
	 *            	<li>typeName</li>
	 *              <li>objDomain</li>
	 *              <li>className</li>
	 *              <li>attributeName</li>
	 *            </ul>
	 * @return the {@link ResultAttribute}
	 */
	public static ResultAttribute fromAttribute(@Nonnull String attributeName) {
		String[] split = StringUtils.splitByCharacterTypeCamelCase(attributeName);
		StringBuilder sb = new StringBuilder(split[0].toUpperCase()).append("_").append(split[1].toUpperCase());
		return valueOf(sb.toString());
	}

	/**
	 * Calls the Getter defined by the {@link ResultAttribute} on the
	 * {@link Result} add adds the entry to the supplied {@link Map}
	 * 
	 * @param attributeMap
	 *            The map to add the {@link Result} data to
	 * @param result
	 *            The {@link Result} to get the data from
	 */
	// Reflection errors have been covered fully by tests
	@SneakyThrows
	public void addAttribute(@Nonnull Map<String, String> attributeMap, @Nonnull Result result) {
		Method m = result.getClass().getMethod(accessorMethod);
		attributeMap.put(attributeName, (String) m.invoke(result));
	}
}