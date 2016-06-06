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
package com.googlecode.jmxtrans.model.output;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MapEntryToDimensionTest {

	private final MapEntryToDimension mapEntryToDimension = new MapEntryToDimension();

	@Test
	public void simpleDimensionIsCreated() {
		Dimension dimension = mapEntryToDimension.apply(ImmutableMap.of(
				"name", (Object) "some_name",
				"value", (Object)"some_value"));

		assertThat(dimension.getName()).isEqualTo("some_name");
		assertThat(dimension.getValue()).isEqualTo("some_value");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nameMustBeGiven() {
		Dimension dimension = mapEntryToDimension.apply(ImmutableMap.of(
				"no_name", (Object) "some_name",
				"value", (Object)"some_value"));

		assertThat(dimension.getName()).isEqualTo("some_name");
		assertThat(dimension.getValue()).isEqualTo("some_value");
	}

	@Test(expected = IllegalArgumentException.class)
	public void valueMustBeGiven() {
		Dimension dimension = mapEntryToDimension.apply(ImmutableMap.of(
				"name", (Object) "some_name",
				"no_value", (Object)"some_value"));

		assertThat(dimension.getName()).isEqualTo("some_name");
		assertThat(dimension.getValue()).isEqualTo("some_value");
	}

	@Test
	@Ignore("Should run on EC2 to be actually relevant")
	public void dimensionIsCreatedFromEC2Metadata() {
		Dimension dimension = mapEntryToDimension.apply(ImmutableMap.of(
				"name", (Object) "some_name",
				"value", (Object)"$AmiId"));

		assertThat(dimension.getName()).isEqualTo("some_name");
		assertThat(dimension.getValue()).isEqualTo("null");
	}

}
