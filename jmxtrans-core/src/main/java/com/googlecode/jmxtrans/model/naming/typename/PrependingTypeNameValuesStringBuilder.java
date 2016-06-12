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

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

@Immutable
@EqualsAndHashCode
public class PrependingTypeNameValuesStringBuilder extends TypeNameValuesStringBuilder {

	private final ImmutableList<String> prependedTypeNames;

	public PrependingTypeNameValuesStringBuilder(String separator, List<String> prependedTypeNames) {
		super(separator);
		this.prependedTypeNames = copyOf(prependedTypeNames);
	}

	@Override
	public String build(List<String> typeNames, String typeNameStr) {
		List<String> resultingTypeNames = new ArrayList<>(prependedTypeNames);
		if (typeNames != null) {
			for (String name : typeNames) {
				if (!resultingTypeNames.contains(name)) {
					resultingTypeNames.add(name);
				}
			}
		}
		return doBuild(resultingTypeNames, typeNameStr);
	}
}
