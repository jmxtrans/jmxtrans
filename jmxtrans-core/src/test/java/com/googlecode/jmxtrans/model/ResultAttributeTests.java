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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.WordUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

/**
 *
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResultAttributeTests {

	private Map<String, String> attributeMap;
	Result result = new Result(2l, "attributeName1", "className1", "objDomain1", "keyAlias1", "type=Type1,name=Name1",
			ImmutableList.of("key"), 1);

	@Before
	public void setup() {
		attributeMap = new HashMap<String, String>();
	}

	@Test
	public void attributeNamesFromResultAreWrittenToMap() throws Exception {
		for (ResultAttribute resultAttribute : ResultAttributes.values()) {
			resultAttribute.addTo(attributeMap, result);
			String attributeName = resultAttribute.getName();
			Method m = result.getClass().getMethod("get" + WordUtils.capitalize(attributeName));
			String expectedValue = (String) m.invoke(result);
			assertThat(attributeMap).containsEntry(attributeName, expectedValue);
		}
	}

	@Test
	public void typeNamePropertyResultAttribute() {
		ResultAttribute typeAttr = ResultAttributes.forName("typeName.type");
		assertThat(typeAttr.getName()).isEqualTo("typeName.type");
		String type = typeAttr.get(result);
		assertThat(type).isEqualTo("Type1");

		ResultAttribute nameAttr = ResultAttributes.forName("typeName.name");
		assertThat(nameAttr.getName()).isEqualTo("typeName.name");
		String name = nameAttr.get(result);
		assertThat(name).isEqualTo("Name1");

		ResultAttribute notExistAttr = ResultAttributes.forName("typeName.notExist");
		assertThat(notExistAttr.getName()).isEqualTo("typeName.notExist");
		String notExist = notExistAttr.get(result);
		assertThat(notExist).isNull();

		try {
			ResultAttributes.forName("notExist");
			fail("Exception expected");
		} catch (IllegalArgumentException e) {

		}
	}
}
