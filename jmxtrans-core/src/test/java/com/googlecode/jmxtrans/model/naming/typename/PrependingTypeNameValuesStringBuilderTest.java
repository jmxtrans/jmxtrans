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

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PrependingTypeNameValuesStringBuilderTest {

	@Test
	public void testBuilds() {
		PrependingTypeNameValuesStringBuilder builder = new PrependingTypeNameValuesStringBuilder(
				TypeNameValuesStringBuilder.DEFAULT_SEPARATOR, asList("p-key1-p", "q-key1-q"));
		assertThat(builder.build(null, "p-key1-p=p-value1-p,q-key1-q=q-value1-q")).isEqualTo("p-value1-p_q-value1-q");
		assertThat(builder.build(Collections.<String>emptyList(), "p-key1-p=p-value1-p")).isEqualTo("p-value1-p");
		assertThat(builder.build(asList("x-key1-x"), "")).isEmpty();
		assertThat(builder.build(asList("x-key1-x"), "p-key1-p=p-value1-p")).isEqualTo("p-value1-p");
		assertThat(builder.build(asList("x-key1-x", "y-key2-y"), "")).isEmpty();
		assertThat(builder.build(asList("x-key1-x"), "x-key1-x=x-value1-x")).isEqualTo("x-value1-x");
		assertThat(builder.build(asList("x-key1-x"), "x-key1-x=x-value1-x,q-key1-q=q-value1-q"))
				.isEqualTo("q-value1-q_x-value1-x");
		assertThat(builder.build(asList("x-key1-x"), "y-key2-y=y-value2-y")).isEmpty();
		assertThat(builder.build(asList("x-key1-x", "y-key2-y"), "x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo("x-value1-x_y-value2-y");
		assertThat(builder.build(
					asList("x-key1-x", "y-key2-y"),
					"x-key1-x=x-value1-x,q-key1-q=q-value1-q,y-key2-y=y-value2-y,p-key1-p=p-value1-p"))
				.isEqualTo("p-value1-p_q-value1-q_x-value1-x_y-value2-y");
	}
}
