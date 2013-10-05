package com.googlecode.jmxtrans.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author lanyonm
 */
public class PropertyResolverTests {

	@Test
	public void testResolveMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		assertEquals("w2", System.getProperty("myhost"));
		assertEquals(null, System.getProperty("mihost"));
		map.put("a", "${myhost}");
		map.put("b", "${mihost:w4}");
		map.put("c", "${mybean:defbean}.${mybean2:defbean2}");
		map.put("d", "${myhost:defbean}.${mybean2:defbean2}");
		PropertyResolver.resolveMap(map);
		assertEquals("w2", map.get("a"));
		assertEquals("w4", map.get("b"));
		assertEquals("defbean.defbean2", map.get("c"));
		assertEquals("w2.defbean2", map.get("d"));
	}
}
