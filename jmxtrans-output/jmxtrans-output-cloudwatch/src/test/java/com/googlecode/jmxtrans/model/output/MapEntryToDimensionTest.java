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
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EC2MetadataUtils.class})
public class MapEntryToDimensionTest {

	private final MapEntryToDimension mapEntryToDimension = new MapEntryToDimension();
	private final String amiId = UUID.randomUUID().toString();

	@Before
	public void mockAmazonAPI() throws Exception {
		mockStatic(EC2MetadataUtils.class);
		when(EC2MetadataUtils.getAmiId()).thenReturn(amiId);
		when(EC2MetadataUtils.getAmiLaunchIndex()).thenThrow(new RuntimeException());
	}

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
	//@Ignore("Should run on EC2 to be actually relevant")
	public void dimensionIsCreatedFromEC2Metadata() {
		Map<String,String> tests = ImmutableMap.of(
				"$AmiId", amiId,
				"$UnknownField", "$UnknownField",
				"$AmiLaunchIndex", "$AmiLaunchIndex"
		);

		for (Map.Entry<String,String> test : tests.entrySet()) {
			Dimension dimension = mapEntryToDimension.apply(ImmutableMap.of(
					"name", (Object) "some_name",
					"value", (Object) test.getKey()));

			assertThat(dimension.getName()).isEqualTo("some_name");
			assertThat(dimension.getValue()).isEqualTo(test.getValue());
		}



	}

}
