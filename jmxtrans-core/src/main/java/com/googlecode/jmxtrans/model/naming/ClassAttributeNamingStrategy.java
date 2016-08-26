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

import com.googlecode.jmxtrans.model.NamingStrategy;
import com.googlecode.jmxtrans.model.Result;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Strategy for naming metrics, tags, and the like given a result.
 */

@EqualsAndHashCode
public class ClassAttributeNamingStrategy implements NamingStrategy {
	@Getter @Setter protected String delimiter = ".";

	@Override
	public String formatName(Result result) {
		StringBuilder formatted = new StringBuilder();
		String attName = result.getAttributeName();
		String className = result.getKeyAlias();

		if (className == null)
			className = result.getClassName();

		formatted.append(className);
		formatted.append(delimiter);
		formatted.append(attName);

		return formatted.toString();
	}
}
