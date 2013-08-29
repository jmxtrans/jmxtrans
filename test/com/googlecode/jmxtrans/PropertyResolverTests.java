package com.googlecode.jmxtrans;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import com.googlecode.jmxtrans.util.PropertyResolver;

public class PropertyResolverTests {

	@Test
	public void testProps() {
		System.setProperty("myhost", "w2");
		System.setProperty("myport", "1099");

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
		map.put("count", new Integer(10));

		PropertyResolver.resolveMap(map);
		Assert.assertEquals("w2", map.get("host"));
		Assert.assertEquals("1099", map.get("port"));
		Assert.assertEquals(new Integer(10), map.get("count"));
	}

	@Test
	public void testList() {
		List<String> list = new ArrayList<String>();
		list.add("${myhost}");
		list.add("${myport}");
		list.add("count");

		PropertyResolver.resolveList(list);
		Assert.assertEquals("w2", list.get(0));
		Assert.assertEquals("1099", list.get(1));
		Assert.assertEquals("count", list.get(2));
	}

}
