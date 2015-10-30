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
package com.googlecode.jmxtrans.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * 
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResultAttributeTests {
	
	private Map<String, String> attributeMap;
	private static final String VALUE_ATTRIBUTE_NAME = "attributeName1";
	private static final String VALUE_CLASS_NAME = "className1";
	private static final String VALUE_OBJ_DOMAIN = "objDomain1";
	private static final String VALUE_TYPE_NAME = "typeName1";
	@Mock Result result;
	
	@Before
	public void setup() {
		attributeMap = new HashMap<String, String>();
		when(result.getAttributeName()).thenReturn(VALUE_ATTRIBUTE_NAME);
		when(result.getClassName()).thenReturn(VALUE_CLASS_NAME);
		when(result.getObjDomain()).thenReturn(VALUE_OBJ_DOMAIN);
		when(result.getTypeName()).thenReturn(VALUE_TYPE_NAME);
	}
	
	@Test
	public void nothingIsAddedToTheAttributeMapForANullResult() throws Exception {
		assertThat(attributeMap).isEmpty();
		ResultAttribute.ATTRIBUTENAME.addAttribute(attributeMap, null);
		assertThat(attributeMap).isEmpty();
	}
	
	@Test
	public void attributeNameFromResultCanBeWrittenToMap() throws Exception {
		ResultAttribute resultAttribute = ResultAttribute.ATTRIBUTENAME;
		assertThat(attributeMap).isEmpty();
		resultAttribute.addAttribute(attributeMap, result);
		ensureThatResultAttributeValueIsInMap(resultAttribute, VALUE_ATTRIBUTE_NAME);	
	}
	
	@Test
	public void classNameFromResultCanBeWrittenToMap() throws Exception {
		ResultAttribute resultAttribute = ResultAttribute.CLASSNAME;
		assertThat(attributeMap).isEmpty();
		resultAttribute.addAttribute(attributeMap, result);
		ensureThatResultAttributeValueIsInMap(resultAttribute, VALUE_CLASS_NAME);	
	}
	
	@Test
	public void objDomainFromResultCanBeWrittenToMap() throws Exception {
		ResultAttribute resultAttribute = ResultAttribute.OBJDOMAIN;
		assertThat(attributeMap).isEmpty();
		resultAttribute.addAttribute(attributeMap, result);
		ensureThatResultAttributeValueIsInMap(resultAttribute, VALUE_OBJ_DOMAIN);	
	}
	
	@Test
	public void typeNameFromResultCanBeWrittenToMap() throws Exception {
		ResultAttribute resultAttribute = ResultAttribute.TYPENAME;
		assertThat(attributeMap).isEmpty();
		resultAttribute.addAttribute(attributeMap, result);
		ensureThatResultAttributeValueIsInMap(resultAttribute, VALUE_TYPE_NAME);	
	}
	
	private void ensureThatResultAttributeValueIsInMap(ResultAttribute resultAttribute, String expectedValue) {
		assertThat(attributeMap).containsEntry(resultAttribute.getAttributeName(), expectedValue);
	}
}