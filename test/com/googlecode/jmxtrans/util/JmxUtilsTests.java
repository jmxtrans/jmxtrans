package com.googlecode.jmxtrans.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Zack Radick Date: 1/20/12
 * @author Arthur Naseef Date: 02/23/2014
 */
public class JmxUtilsTests {
	@Test
	public void testIsNumeric() {
		assertFalse(JmxUtils.isNumeric(null));
		assertTrue(JmxUtils.isNumeric("")); // this is "true" for historical
											// reasons
		assertFalse(JmxUtils.isNumeric("  "));
		assertTrue(JmxUtils.isNumeric("123"));
		assertFalse(JmxUtils.isNumeric("12 3"));
		assertFalse(JmxUtils.isNumeric("ab2c"));
		assertFalse(JmxUtils.isNumeric("12-3"));
		assertTrue(JmxUtils.isNumeric("12.3"));
		assertFalse(JmxUtils.isNumeric("12.3.3.3"));
		assertTrue(JmxUtils.isNumeric(".2"));
		assertFalse(JmxUtils.isNumeric("."));
	}

	@Test
	public void testGetTypeNameValueMap () {
		assertEquals(java.util.Collections.EMPTY_MAP, JmxUtils.getTypeNameValueMap(null));
		assertEquals(java.util.Collections.EMPTY_MAP, JmxUtils.getTypeNameValueMap(""));
		assertEquals(makeMap("x-key1-x", ""), JmxUtils.getTypeNameValueMap("x-key1-x"));
		assertEquals(makeMap("x-key1-x", "", "x-key2-x", ""),
		             JmxUtils.getTypeNameValueMap("x-key1-x,x-key2-x"));
		assertEquals(makeMap("x-key1-x", "x-value1-x"),
		             JmxUtils.getTypeNameValueMap("x-key1-x=x-value1-x"));
		assertEquals(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "y-value2-y"),
		             JmxUtils.getTypeNameValueMap("x-key1-x=x-value1-x,y-key2-y=y-value2-y"));
		assertEquals(
			makeMap("x-key1-x", "x-value1-x", "y-key2-y", "y-value2-y", "z-key3-z", "z-value3-z"),
			JmxUtils.getTypeNameValueMap("x-key1-x=x-value1-x,y-key2-y=y-value2-y,z-key3-z=z-value3-z"));
		assertEquals(
			makeMap("x-key1-x", "x-value1-x", "y-key2-y", "", "yy-key2.5-yy", "a=1",
			        "z-key3-z", "z-value3-z"),
			JmxUtils.getTypeNameValueMap(
				"x-key1-x=x-value1-x,y-key2-y,yy-key2.5-yy=a=1,z-key3-z=z-value3-z"));
	}


	@Test
	public void testGetConcatenatedTypeNameValues () {
		assertNull(JmxUtils.getConcatedTypeNameValues(null, "a=1"));
		assertNull(JmxUtils.getConcatedTypeNameValues(Collections.EMPTY_LIST, "a=1"));
		assertEquals("", JmxUtils.getConcatedTypeNameValues(Arrays.asList("x-key1-x"), ""));
		assertEquals("", JmxUtils.getConcatedTypeNameValues(Arrays.asList("x-key1-x", "y-key2-y"), ""));
		assertEquals("x-value1-x",
		             JmxUtils.getConcatedTypeNameValues(Arrays.asList("x-key1-x"), "x-key1-x=x-value1-x"));
		assertEquals("", JmxUtils.getConcatedTypeNameValues(Arrays.asList("x-key1-x"), "y-key2-y=y-value2-y"));
		assertEquals("x-value1-x_y-value2-y",
		             JmxUtils.getConcatedTypeNameValues(Arrays.asList("x-key1-x", "y-key2-y"),
		                                                "x-key1-x=x-value1-x,y-key2-y=y-value2-y"));
	}

	/**
	 * Convenience method for creating a Map for comparison.
	 */
	protected Map<String, String>   makeMap (String... keysAndValues) {
		Map<String, String> result;
		int                 cur;

		result = new HashMap<String, String>();

		cur = 0;
		while ( cur < keysAndValues.length ) {
			if ( cur < keysAndValues.length - 1 ) {
				result.put(keysAndValues[cur], keysAndValues[cur + 1]);
				cur += 2;
			}
			else {
				result.put(keysAndValues[cur], "");
				cur++;
			}
		}

		return	result;
	}
}
