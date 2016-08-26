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
package com.googlecode.jmxtrans.model.results;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.results.ValueTransformer;

import javax.annotation.Nullable;

public class ResultValuesTransformer implements Function<Result, Result> {

	private final ValueTransformer valueTransformer;

	public ResultValuesTransformer(ValueTransformer valueTransformer) {
		this.valueTransformer = valueTransformer;
	}

	@Nullable
	@Override
	public Result apply(@Nullable Result input) {
		if (input == null) {
			return null;
		}
		return new Result(
				input.getEpoch(),
				input.getAttributeName(),
				input.getClassName(),
				input.getObjDomain(),
				input.getKeyAlias(),
				input.getTypeName(),
				Maps.transformValues(input.getValues(), valueTransformer)
		);
	}

}
