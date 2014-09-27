package com.googlecode.jmxtrans.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;

/**
 * @author lanyonm
 */
public class PropertyResolverTests {

	@Before
	public void setSomeProperties() {
		System.setProperty("myhost", "w2");
		System.setProperty("myport", "1099");
	}

	@Test
	public void testProps() {
		String s1 = "${xxx} : ${yyy}";
		String s2 = PropertyResolver.resolveProps(s1);
		Assert.assertEquals(s1, s2);

		s1 = "${myhost} : ${myport}";
		s2 = PropertyResolver.resolveProps(s1);
		Assert.assertEquals("w2 : 1099", s2);

		s1 = "${myhost} : ${myaltport:2099}";
		s2 = PropertyResolver.resolveProps(s1);
		Assert.assertEquals("w2 : 2099", s2);
		s1 = "${myhost}:2099";
		s2 = PropertyResolver.resolveProps(s1);
		Assert.assertEquals("w2:2099", s2);

	}

	@Test
	public void testMap() {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		map.put("host", "${myhost}");
		map.put("port", "${myport}");
		map.put("count", 10);

		PropertyResolver.resolveMap(map);
		Assert.assertEquals("w2", map.get("host"));
		Assert.assertEquals("1099", map.get("port"));
		Assert.assertEquals(10, map.get("count"));
	}

	@Test
	public void testResolveMap() {
		Map<String, Object> map = newHashMap();
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

	@Test
	public void testList() {
		List<String> list = new ArrayList<String>();
		list.add("${myhost}");
		list.add("${myport}");
		list.add("count");

		List<String> resolvedList = PropertyResolver.resolveList(list);
		Assert.assertEquals("w2", resolvedList.get(0));
		Assert.assertEquals("1099", resolvedList.get(1));
		Assert.assertEquals("count", resolvedList.get(2));
	}

	@After
	public void removeSystemProperties() {
		System.clearProperty("myhost");
		System.clearProperty("myport");
	}

}
