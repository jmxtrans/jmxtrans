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
package com.googlecode.jmxtrans.jmx;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>Warning:</b> This test class relies on MBeans exposed by the JVM. As far
 * as I understand the documentation, it only relies on beans and attributes
 * that should be present on all implementations. Still there is some chance
 * that this test will fail on some JVMs or that it depends too much on
 * specific platform properties.
 */
public class JmxResultProcessorTest {

	private Query query;
	private final static String TEST_DOMAIN_NAME = "ObjectDomainName";

	@Before
	public void initConfiguration() {
		query = Query.builder()
				.setResultAlias("resultAlias")
				.build();
	}

	@Test
	public void canCreateBasicResultData() throws MalformedObjectNameException, InstanceNotFoundException {
		Attribute integerAttribute = new Attribute("StartTime", 51L);
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				query,
				runtime,
				ImmutableList.of(integerAttribute),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result integerResult = results.get(0);

		assertThat(integerResult.getAttributeName()).isEqualTo("StartTime");
		assertThat(integerResult.getClassName()).isEqualTo("sun.management.RuntimeImpl");
		assertThat(integerResult.getKeyAlias()).isEqualTo("resultAlias");
		assertThat(integerResult.getTypeName()).isEqualTo("type=Runtime");
		assertThat(integerResult.getValues()).hasSize(1);
	}

	@Test
	public void doesNotReorderTypeNames() throws MalformedObjectNameException {
		String className = "java.lang.SomeClass";
		String propertiesOutOfOrder = "z-key=z-value,a-key=a-value,k-key=k-value";
		List<Result> results = new JmxResultProcessor(
				query,
				new ObjectInstance(className + ":" + propertiesOutOfOrder, className),
				ImmutableList.of(new Attribute("SomeAttribute", 1)),
				className,
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result integerResult = results.get(0);
		assertThat(integerResult.getTypeName()).isEqualTo(propertiesOutOfOrder);

	}

	@Test
	public void canReadSingleIntegerValue() throws MalformedObjectNameException, InstanceNotFoundException {
		Attribute integerAttribute = new Attribute("CollectionCount", 51L);
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				query,
				runtime,
				ImmutableList.of(integerAttribute),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result integerResult = results.get(0);

		assertThat(integerResult.getAttributeName()).isEqualTo("CollectionCount");
		assertThat(integerResult.getValues()).hasSize(1);

		Object objectValue = integerResult.getValues().get("CollectionCount");
		assertThat(objectValue).isInstanceOf(Long.class);

		Long integerValue = (Long) objectValue;
		assertThat(integerValue).isEqualTo(51L);
	}

	@Test
	public void canReadSingleBooleanValue() throws MalformedObjectNameException, InstanceNotFoundException {
		Attribute booleanAttribute = new Attribute("BootClassPathSupported", true);
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				query,
				runtime,
				ImmutableList.of(booleanAttribute),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result result = results.get(0);

		assertThat(result.getAttributeName()).isEqualTo("BootClassPathSupported");
		assertThat(result.getValues()).hasSize(1);

		Object objectValue = result.getValues().get("BootClassPathSupported");
		assertThat(objectValue).isInstanceOf(Boolean.class);

		Boolean booleanValue = (Boolean) objectValue;
		assertThat(booleanValue).isEqualTo(TRUE);
	}

	@Test
	public void canReadTabularData() throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
			ReflectionException, InstanceNotFoundException {
		ObjectInstance runtime = getRuntime();
		AttributeList attr = ManagementFactory.getPlatformMBeanServer().getAttributes(
				runtime.getObjectName(), new String[]{"SystemProperties"});
		List<Result> results = new JmxResultProcessor(
				query,
				runtime,
				attr.asList(),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results.size()).isGreaterThan(2);

		Optional<Result> result = from(results).firstMatch(new ByAttributeName("SystemProperties.java.version"));

		assertThat(result.isPresent()).isTrue();

		assertThat(result.get().getAttributeName()).isEqualTo("SystemProperties.java.version");
		Map<String,Object> values = result.get().getValues();
		assertThat(values).hasSize(2);

		Object objectValue = result.get().getValues().get("key");
		assertThat(objectValue).isInstanceOf(String.class);

		String key = (String) objectValue;
		assertThat(key).isEqualTo("java.version");
	}

	@Test
	public void canReadCompositeData() throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
			ReflectionException, InstanceNotFoundException {
		ObjectInstance memory = getMemory();
		AttributeList attr = ManagementFactory.getPlatformMBeanServer().getAttributes(
				memory.getObjectName(), new String[]{"HeapMemoryUsage"});
		List<Result> results = new JmxResultProcessor(
				query,
				memory,
				attr.asList(),
				memory.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);

		Result result = results.get(0);

		assertThat(result.getAttributeName()).isEqualTo("HeapMemoryUsage");
		assertThat(result.getTypeName()).isEqualTo("type=Memory");

		Map<String,Object> values = result.getValues();
		assertThat(values).hasSize(4);

		Object objectValue = result.getValues().get("init");
		assertThat(objectValue).isInstanceOf(Long.class);
	}

	@Test
	public void canReadMapData() throws MalformedObjectNameException {
		Attribute mapAttribute = new Attribute("map", ImmutableMap.of("key1", "value1", "key2", "value2"));

		List<Result> results = new JmxResultProcessor(
				query,
				new ObjectInstance("java.lang:type=Memory", "java.lang.SomeClass"),
				ImmutableList.of(mapAttribute),
				"java.lang.SomeClass",
				TEST_DOMAIN_NAME
		).getResults();

		assertThat(results).isNotNull();
		assertThat(results).hasSize(1);
		Result result = results.get(0);
		assertThat(result.getValues()).hasSize(2);
		assertThat(result.getValues()).isEqualTo(ImmutableMap.of("key1", "value1", "key2", "value2"));
	}

	@Test
	public void canReadMapDataWithNonStringKeys() throws MalformedObjectNameException {
		Attribute mapAttribute = new Attribute("map", ImmutableMap.of(1, "value1", 2, "value2"));

		List<Result> results = new JmxResultProcessor(
				query,
				new ObjectInstance("java.lang:type=Memory", "java.lang.SomeClass"),
				ImmutableList.of(mapAttribute),
				"java.lang.SomeClass",
				TEST_DOMAIN_NAME
		).getResults();

		assertThat(results).isNotNull();
		assertThat(results).hasSize(1);
		Result result = results.get(0);
		assertThat(result.getValues()).hasSize(2);
		assertThat(result.getValues()).isEqualTo(ImmutableMap.of("1", "value1", "2", "value2"));
	}

	public ObjectInstance getRuntime() throws MalformedObjectNameException, InstanceNotFoundException {
		return ManagementFactory.getPlatformMBeanServer().getObjectInstance(
				new ObjectName("java.lang", "type", "Runtime"));
	}

	public ObjectInstance getMemory() throws MalformedObjectNameException, InstanceNotFoundException {
		return ManagementFactory.getPlatformMBeanServer().getObjectInstance(
				new ObjectName("java.lang", "type", "Memory"));
	}

	private static class ByAttributeName implements Predicate<Result> {
		private final String attributeName;

		public ByAttributeName(String attributeName) {
			this.attributeName = attributeName;
		}

		@Override
		public boolean apply(@Nullable Result result) {
			if (result == null) {
				return false;
			}
			return attributeName.equals(result.getAttributeName());
		}
	}
}
