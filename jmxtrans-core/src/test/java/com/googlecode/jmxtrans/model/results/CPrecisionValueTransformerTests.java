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

import static org.assertj.core.api.Assertions.assertThat;

public class CPrecisionValueTransformerTests {

	@Test
	public void valueAbovePrecisionIsNotTransformed() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(10);

		assertThat(transformed).isEqualTo(10);
	}

	@Test
	public void negativeValueAbovePrecisionIsNotTransformed() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(-10);

		assertThat(transformed).isEqualTo(-10);
	}

	@Test
	public void valueBelowPrecisionIsTransformedToZero() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(Double.MIN_VALUE);

		assertThat(transformed).isEqualTo(0);
	}

	@Test
	public void nonNumberIsReturnedUnmodified() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply("my value");

		assertThat(transformed).isEqualTo("my value");
	}

	@Test
	public void nullIsReturnedUnmodified() {
		ValueTransformer transformer = new CPrecisionValueTransformer();
		Object transformed = transformer.apply(null);

		assertThat(transformed).isNull();
	}

}
