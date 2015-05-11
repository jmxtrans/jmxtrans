package com.googlecode.jmxtrans.util;

import org.junit.Assert;
import org.junit.Test;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.lang.Boolean.FALSE;

public class NumberUtilsTest {
	@Test
	public void testIsNumeric() {
		Assert.assertFalse(isNumeric(null));
		Assert.assertTrue(isNumeric("")); // this is "true" for historical
		// reasons
		Assert.assertFalse(isNumeric("  "));
		Assert.assertTrue(isNumeric("123"));
		Assert.assertFalse(isNumeric("12 3"));
		Assert.assertFalse(isNumeric("ab2c"));
		Assert.assertFalse(isNumeric("12-3"));
		Assert.assertTrue(isNumeric("12.3"));
		Assert.assertFalse(isNumeric("12.3.3.3"));
		Assert.assertTrue(isNumeric(".2"));
		Assert.assertFalse(isNumeric("."));
		Assert.assertFalse(isNumeric("3."));
		Assert.assertTrue(isNumeric(1L));
		Assert.assertTrue(isNumeric(2));
		Assert.assertTrue(isNumeric((Object) "3.2"));
		Assert.assertFalse(isNumeric((Object) "abc"));
		Assert.assertFalse(isNumeric(FALSE));
	}
}
