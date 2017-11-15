/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.JmxResultProcessor;
import com.googlecode.jmxtrans.model.Result;
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

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenDataException;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.google.common.collect.FluentIterable.from;
import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQueryWithResultAlias;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;

/**
 * <b>Warning:</b> This test class relies on MBeans exposed by the JVM. As far
 * as I understand the documentation, it only relies on beans and attributes
 * that should be present on all implementations. Still there is some chance
 * that this test will fail on some JVMs or that it depends too much on
 * specific platform properties.
 */
public class JmxResultProcessorTest {

	private final static String TEST_DOMAIN_NAME = "ObjectDomainName";

	@Test
	public void canCreateBasicResultData() throws MalformedObjectNameException, InstanceNotFoundException {
		Attribute integerAttribute = new Attribute("StartTime", 51L);
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
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
		assertThat(integerResult.getValue()).isEqualTo(51L);
	}

	@Test
	public void doesNotReorderTypeNames() throws MalformedObjectNameException {
		String className = "java.lang.SomeClass";
		String propertiesOutOfOrder = "z-key=z-value,a-key=a-value,k-key=k-value";
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				new ObjectInstance(className + ":" + propertiesOutOfOrder, className),
				ImmutableList.of(new Attribute("SomeAttribute", 1)),
				className,
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result integerResult = results.get(0);
		assertThat(integerResult.getTypeName()).isEqualTo(propertiesOutOfOrder);

	}

	@Test
	public void testNullValueInCompositeData() throws MalformedObjectNameException, OpenDataException, InstanceNotFoundException {
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				runtime,
				ImmutableList.of(new Attribute("SomeAttribute", getCompositeData())),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();
		assertThat(results).hasSize(0);
	}

	@Test
	public void canReadSingleIntegerValue() throws MalformedObjectNameException, InstanceNotFoundException {
		Attribute integerAttribute = new Attribute("CollectionCount", 51L);
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				runtime,
				ImmutableList.of(integerAttribute),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result integerResult = results.get(0);

		assertThat(integerResult.getAttributeName()).isEqualTo("CollectionCount");
		assertThat(integerResult.getValue()).isInstanceOf(Long.class);
		assertThat(integerResult.getValue()).isEqualTo(51L);
	}

	@Test
	public void canReadSingleBooleanValue() throws MalformedObjectNameException, InstanceNotFoundException {
		Attribute booleanAttribute = new Attribute("BootClassPathSupported", true);
		ObjectInstance runtime = getRuntime();
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				runtime,
				ImmutableList.of(booleanAttribute),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(1);
		Result result = results.get(0);

		assertThat(result.getAttributeName()).isEqualTo("BootClassPathSupported");
		assertThat(result.getValue()).isInstanceOf(Boolean.class);
		assertThat(result.getValue()).isEqualTo(TRUE);
	}

	private Optional<Result> firstMatch(List<Result> results, String attributeName, String  ... valuePath) {
		return from(results).firstMatch(Predicates.and(
				new ByAttributeName(attributeName),
				new ByValuePath(valuePath)));
	}

	@Test
	public void canReadTabularData() throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
			ReflectionException, InstanceNotFoundException {
		ObjectInstance runtime = getRuntime();
		AttributeList attr = ManagementFactory.getPlatformMBeanServer().getAttributes(
				runtime.getObjectName(), new String[]{"SystemProperties"});
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				runtime,
				attr.asList(),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results.size()).isGreaterThan(2);

		Optional<Result> result = firstMatch(results, "SystemProperties", "java.version", "value");

		assertThat(result.isPresent()).isTrue();

		assertThat(result.get().getAttributeName()).isEqualTo("SystemProperties");
		assertThat(result.get().getValuePath()).isEqualTo(ImmutableList.of("java.version", "value"));
		assertThat(result.get().getValue()).isEqualTo(System.getProperty("java.version"));
	}

	@Test(timeout = 1000)
	public void canReadFieldsOfTabularData() throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
			ReflectionException, InstanceNotFoundException {
		// Need to induce a GC for the attribute below to be populated
		Runtime.getRuntime().gc();

		ObjectInstance runtime = null;
		try {
			runtime = getG1YoungGen();
		} catch (InstanceNotFoundException e) {
			// ignore test if G1 not enabled
			assumeNoException("G1 GC in Java 7/8 needs to be enabled with -XX:+UseG1GC", e);
		}

		AttributeList attr;
		// but takes a non-deterministic amount of time for LastGcInfo to get populated
		while (true) { // but bounded by Test timeout
			attr = ManagementFactory.getPlatformMBeanServer().getAttributes(
					runtime.getObjectName(), new String[]{"LastGcInfo"});
			if (((Attribute) attr.get(0)).getValue() != null) {
				break;
			}
		}

		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				runtime,
				attr.asList(),
				runtime.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results.size()).isGreaterThan(2);

		// Should have primitive typed fields
		assertThat(firstMatch(results, "LastGcInfo", "duration").get()).isNotNull();
		// assert tabular fields are excluded
		assertThat(firstMatch(results, "LastGcInfo", "memoryUsageBeforeGc").get()).isNull();
		assertThat(firstMatch(results, "LastGcInfo", "memoryUsageAfterGc").get()).isNull();
	}

	@Test
	public void canReadCompositeData() throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
			ReflectionException, InstanceNotFoundException {
		ObjectInstance memory = getMemory();
		AttributeList attr = ManagementFactory.getPlatformMBeanServer().getAttributes(
				memory.getObjectName(), new String[]{"HeapMemoryUsage"});
		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				memory,
				attr.asList(),
				memory.getClassName(),
				TEST_DOMAIN_NAME).getResults();

		assertThat(results).hasSize(4);

		for(Result result: results) {
			assertThat(result.getAttributeName()).isEqualTo("HeapMemoryUsage");
			assertThat(result.getTypeName()).isEqualTo("type=Memory");
		}

		Optional<Result> optionalResult = firstMatch(results, "HeapMemoryUsage", "init");
		assertThat(optionalResult.isPresent()).isTrue();
		Object objectValue = optionalResult.get().getValue();
		assertThat(objectValue).isInstanceOf(Long.class);
	}

	@Test
	public void canReadMapData() throws MalformedObjectNameException {
		Attribute mapAttribute = new Attribute("map", ImmutableMap.of("key1", "value1", "key2", "value2"));

		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				new ObjectInstance("java.lang:type=Memory", "java.lang.SomeClass"),
				ImmutableList.of(mapAttribute),
				"java.lang.SomeClass",
				TEST_DOMAIN_NAME
		).getResults();

		assertThat(results).isNotNull();
		assertThat(results).hasSize(2);
		for(Result result: results) {
			assertThat(result.getAttributeName()).isEqualTo("map");
			assertThat(result.getTypeName()).isEqualTo("type=Memory");
		}

		assertThat(firstMatch(results, "map", "key1").get().getValue()).isEqualTo("value1");
		assertThat(firstMatch(results, "map", "key2").get().getValue()).isEqualTo("value2");
	}

	@Test
	public void canReadMapDataWithNonStringKeys() throws MalformedObjectNameException {
		Attribute mapAttribute = new Attribute("map", ImmutableMap.of(1, "value1", 2, "value2"));

		List<Result> results = new JmxResultProcessor(
				dummyQueryWithResultAlias(),
				new ObjectInstance("java.lang:type=Memory", "java.lang.SomeClass"),
				ImmutableList.of(mapAttribute),
				"java.lang.SomeClass",
				TEST_DOMAIN_NAME
		).getResults();

		assertThat(results).isNotNull();
		for(Result result: results) {
			assertThat(result.getAttributeName()).isEqualTo("map");
			assertThat(result.getTypeName()).isEqualTo("type=Memory");
		}

		assertThat(firstMatch(results, "map", "1").get().getValue()).isEqualTo("value1");
		assertThat(firstMatch(results, "map", "2").get().getValue()).isEqualTo("value2");
	}

	public ObjectInstance getRuntime() throws MalformedObjectNameException, InstanceNotFoundException {
		return ManagementFactory.getPlatformMBeanServer().getObjectInstance(
				new ObjectName("java.lang", "type", "Runtime"));
	}

	public ObjectInstance getMemory() throws MalformedObjectNameException, InstanceNotFoundException {
		return ManagementFactory.getPlatformMBeanServer().getObjectInstance(
				new ObjectName("java.lang", "type", "Memory"));
	}

	public ObjectInstance getG1YoungGen() throws MalformedObjectNameException, InstanceNotFoundException {
		final Hashtable<String, String> keyProperties = new Hashtable<>();
		keyProperties.put("type", "GarbageCollector");
		keyProperties.put("name", "G1 Young Generation");
		return ManagementFactory.getPlatformMBeanServer().getObjectInstance(
				new ObjectName("java.lang", keyProperties));
	}

	private CompositeData getCompositeData() throws OpenDataException {
		String[] keys = new String[]{"p1"};
		OpenType[] itemTypes = new OpenType[]{SimpleType.STRING};
		CompositeType compType = new CompositeType("compType", "descr", keys, keys, itemTypes);
		Map<String, Object> values = new HashMap();
		values.put("p1", null);
		return new CompositeDataSupport(compType, values);
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
	private static class ByValuePath implements Predicate<Result> {
		private final ImmutableList<String> valuePath;

		public ByValuePath(String ... valuePath) {
			this.valuePath = ImmutableList.copyOf(valuePath);
		}

		@Override
		public boolean apply(@Nullable Result result) {
			if (result == null) {
				return false;
			}
			return valuePath.equals(result.getValuePath());
		}
	}
}
