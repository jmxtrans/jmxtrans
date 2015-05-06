package com.googlecode.jmxtrans.model.naming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest {

	@Test
	public void testCleanupStr() {
		assertEquals("addfber1241qdw!èé$", StringUtils.cleanupStr("addfber1241qdw!èé$"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd.abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd.abcd."));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr(".abcd_abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd/"));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd_abcd"));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd'_abcd'"));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd\"_abcd\""));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/ab cd_abcd"));
		assertEquals(null, StringUtils.cleanupStr(null));
		assertEquals("", StringUtils.cleanupStr(""));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\".\"\""));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\"_\"\""));
	}

	@Test
	public void testCleanupStrDottedKeysKept() {
		assertEquals("addfber1241qdw!èé$", StringUtils.cleanupStr("addfber1241qdw!èé$", true));
		assertEquals("abcd.abcd", StringUtils.cleanupStr("abcd.abcd", true));
		assertEquals("abcd.abcd", StringUtils.cleanupStr("abcd.abcd.", true));
		assertEquals(".abcd_abcd", StringUtils.cleanupStr(".abcd_abcd", true));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd", true));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd/abcd/", true));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd_abcd", true));
		assertEquals("abcd_abcd", StringUtils.cleanupStr("abcd'_abcd'", true));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/abcd\"_abcd\"", true));
		assertEquals("_abcd_abcd", StringUtils.cleanupStr("/ab cd_abcd", true));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\".\"\""));
		assertEquals("abcd", StringUtils.cleanupStr("\"abcd\"_\"\""));
	}

}
