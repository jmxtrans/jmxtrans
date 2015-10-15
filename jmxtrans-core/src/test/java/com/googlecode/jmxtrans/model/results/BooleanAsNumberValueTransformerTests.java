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
package com.googlecode.jmxtrans.model.results;

import org.junit.Test;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

public class BooleanAsNumberValueTransformerTests {

	@Test
	public void integerIsNotModified() {
		Integer in = 10;
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isSameAs(in);
	}

	@Test
	public void stringIsNotModified() {
		String in = "";
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isSameAs(in);
	}

	@Test
	public void trueIsConvertedToNumber() {
		Boolean in = TRUE;
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isEqualTo(0);
	}

	@Test
	public void falseIsConvertedToNumber() {
		Boolean in = FALSE;
		Object out = new BooleanAsNumberValueTransformer(0, 1).apply(in);

		assertThat(out).isEqualTo(1);
	}

}
