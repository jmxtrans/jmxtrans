package com.googlecode.jmxtrans.util;

import org.junit.Test;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.lang.Boolean.FALSE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NumberUtilsTest {
	@Test
	public void testIsNumeric() {
		assertFalse(isNumeric(null));
		assertTrue(isNumeric("")); // this is "true" for historical
		// reasons
		assertFalse(isNumeric("  "));
		assertTrue(isNumeric("123"));
		assertFalse(isNumeric("12 3"));
		assertFalse(isNumeric("ab2c"));
		assertFalse(isNumeric("12-3"));
		assertTrue(isNumeric("12.3"));
		assertFalse(isNumeric("12.3.3.3"));
		assertTrue(isNumeric(".2"));
		assertFalse(isNumeric("."));
		assertFalse(isNumeric("3."));
		assertTrue(isNumeric(1L));
		assertTrue(isNumeric(2));
		assertTrue(isNumeric((Object) "3.2"));
		assertFalse(isNumeric((Object) "abc"));
		assertFalse(isNumeric(FALSE));
	}
}
