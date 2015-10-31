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

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Enumerates the attributes of {@link Result}
 * 
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 *
 */
public enum ResultAttribute {

	TYPENAME("typeName"), OBJDOMAIN("objDomain"), CLASSNAME("className"), ATTRIBUTENAME("attributeName");

	private String attributeName;
	private String accessorMethod;

	private ResultAttribute(String attributeName) {
		this.attributeName = attributeName;
		this.accessorMethod = "get" + StringUtils.capitalize(attributeName);
	}

	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * Calls the Getter defined by the {@link ResultAttribute} on the
	 * {@link Result} add adds the entry to the supplied {@link Map}
	 * 
	 * @param attributeMap
	 *            The map to add the {@link Result} data to
	 * @param result
	 *            The {@link Result} to get the data from
	 * @throws Exception
	 *             If reflection cannot be performed on the {@link Result}
	 */
	public void addAttribute(@Nonnull Map<String, String> attributeMap, @Nonnull Result result) throws Exception {
		Method m = result.getClass().getMethod(accessorMethod);
		attributeMap.put(attributeName, (String) m.invoke(result));
	}
}