package com.googlecode.jmxtrans.model;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.collect.Maps.newHashMap;

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

		ImmutableMap<String, Object> resolved = PropertyResolver.resolveMap(map);
		Assertions.assertThat(resolved).containsEntry("host", "w2");
		Assertions.assertThat(resolved).containsEntry("port", "1099");
		Assertions.assertThat(resolved).containsEntry("count", 10);
	}

	@Test
	public void testResolveMap() {
		Map<String, Object> map = newHashMap();
		Assertions.assertThat(System.getProperty("myhost")).isEqualTo("w2");
		Assertions.assertThat(System.getProperty("mihost")).isNull();

		map.put("a", "${myhost}");
		map.put("b", "${mihost:w4}");
		map.put("c", "${mybean:defbean}.${mybean2:defbean2}");
		map.put("d", "${myhost:defbean}.${mybean2:defbean2}");

		ImmutableMap<String, Object> resolved = PropertyResolver.resolveMap(map);

		Assertions.assertThat(resolved).containsEntry("a", "w2");
		Assertions.assertThat(resolved).containsEntry("b", "w4");
		Assertions.assertThat(resolved).containsEntry("c", "defbean.defbean2");
		Assertions.assertThat(resolved).containsEntry("d", "w2.defbean2");
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
