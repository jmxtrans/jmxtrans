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

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Attributes of {@link Result}
 *
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 *
 */
@ToString
public abstract class ResultAttribute {

	/**
	 * Attribute name in camel case
	 */
	@Getter
	@Nonnull
	private String attributeName;

	ResultAttribute(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * Get attribute name in upper case snake case
     */
	public String name() {
		String[] words = StringUtils.splitByCharacterTypeCamelCase(attributeName);
		return StringUtils.join(words, "_").replaceAll("_\\._", ".").toUpperCase();
	}
	/**
	 * Get attribute on result
     */
	public abstract String getAttribute(Result result);
	/**
	 * Calls the Getter defined by the {@link #getAttribute(Result)} on the
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
		attributeMap.put(attributeName, getAttribute(result));
	}
}
