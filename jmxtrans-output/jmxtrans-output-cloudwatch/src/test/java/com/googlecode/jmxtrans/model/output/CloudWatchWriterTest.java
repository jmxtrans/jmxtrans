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
package com.googlecode.jmxtrans.model.output;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CloudWatchWriter}.
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CloudWatchWriterTest {

	@Mock
	private Server server;

	@Mock
	private Query query;

	@Mock
	private AmazonCloudWatch amazonCloudWatch;

	private CloudWatchWriter cloudWatchWriter;

	@Test
	public void testInitialization() throws Exception {
		String typeName = "TestTypeName";
		String namespace = "TestNamespace";
		String dimensionName = "TestDimensionName";
		String dimensionValue = "TestDimensionValue";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.of(typeName), false, false,
				namespace, ImmutableList.of(dimensionMap(dimensionName, dimensionValue)),
				ImmutableMap.<String, Object>of());

		assertThat(cloudWatchWriter.getTypeNames()).containsExactly(typeName);
		assertThat(cloudWatchWriter.getNamespace()).isEqualTo(namespace);

		cloudWatchWriter.validateSetup(server, query);

		assertThat(cloudWatchWriter.getDimensions()).containsExactly(dimension(dimensionName, dimensionValue));
	}

	@Test
	public void testInitializationWithSettings() throws Exception {
		String typeName = "TestTypeName";
		String namespace = "TestNamespace";
		String dimensionName = "TestDimensionName";
		String dimensionValue = "TestDimensionValue";

		cloudWatchWriter = new CloudWatchWriter(
				null, false, null,
				null, null,
				ImmutableMap.<String, Object>of(
						"typeNames", ImmutableList.of(typeName), "booleanAsNumber", true, "debug", true,
						"namespace", namespace, "dimensions", ImmutableList.of(dimensionMap(dimensionName, dimensionValue))
				)
		);

		assertThat(cloudWatchWriter.getTypeNames()).containsExactly(typeName);
		assertThat(cloudWatchWriter.getNamespace()).isEqualTo(namespace);

		cloudWatchWriter.validateSetup(server, query);

		assertThat(cloudWatchWriter.getDimensions()).containsExactly(dimension(dimensionName, dimensionValue));
	}

	@Test
	public void testInitializationInvalidNamespace() throws Exception {
		String typeName = "TestTypeName";
		String namespace = "";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.of(typeName), false, false,
				namespace, ImmutableList.of(dimensionMap("TestDimensionName", "TestDimensionValue")),
				ImmutableMap.<String, Object>of());

		assertThat(cloudWatchWriter.getTypeNames()).containsExactly(typeName);
		assertThat(cloudWatchWriter.getNamespace()).isEqualTo(namespace);

		assertThatThrownBy(new ThrowingCallable() {
			@Override
			public void call() throws Throwable {
				cloudWatchWriter.validateSetup(server, query);
			}
		}).isInstanceOf(ValidationException.class);
	}

	@Test
	public void testInitializationInvalidDimensions() throws Exception {
		String typeName = "TestTypeName";
		String namespace = "TestNamespace";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.of(typeName), false, false,
				namespace, ImmutableList.<Map<String, Object>>of(ImmutableMap.<String, Object>of("not_name", "invalid", "not_value", "invalid")),
				ImmutableMap.<String, Object>of());

		assertThat(cloudWatchWriter.getTypeNames()).containsExactly(typeName);
		assertThat(cloudWatchWriter.getNamespace()).isEqualTo(namespace);

		assertThatThrownBy(new ThrowingCallable() {
			@Override
			public void call() throws Throwable {
				cloudWatchWriter.validateSetup(server, query);
			}
		}).isInstanceOf(ValidationException.class);
	}

	@Test
	public void testLifecycle() throws Exception {
		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.of("TestTypeName"), false, false,
				"TestNamespace", ImmutableList.of(dimensionMap("TestDimensionName", "TestDimensionValue")),
				ImmutableMap.<String, Object>of()) {
			@Override
			protected AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);

		cloudWatchWriter.start();

		cloudWatchWriter.close();

		verify(amazonCloudWatch).shutdown();
	}

	@Test
	public void testLifecycleMultiple() throws Exception {
		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.of("TestTypeName"), false, false,
				"TestNamespace", ImmutableList.of(dimensionMap("TestDimensionName", "TestDimensionValue")),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);

		cloudWatchWriter.start();

		cloudWatchWriter.close();

		verify(amazonCloudWatch).shutdown();
		reset(amazonCloudWatch);

		cloudWatchWriter.start();

		cloudWatchWriter.close();

		verify(amazonCloudWatch).shutdown();
	}

	@Test
	public void testInternalWrite() throws Exception {
		String namespace = "TestNamespace";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(), false, false,
				namespace, ImmutableList.<Map<String, Object>>of(),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);
		cloudWatchWriter.start();

		Iterable<Result> results = ImmutableList.of(
				result("TestResultByte", (byte) 123),
				result("TestResultInt", 123),
				result("TestResultLong", 123L),
				result("TestResultFloat", 123.456f),
				result("TestResultDouble", 123.456d)
		);

		cloudWatchWriter.doWrite(server, query, results);

		verify(amazonCloudWatch).putMetricData(argThat(requestWith(namespace, hasMetrics(
				metricWith("TestResultByte", 123.0),
				metricWith("TestResultInt", 123.0),
				metricWith("TestResultLong", 123.0),
				metricWith("TestResultFloat", 123.456f), // rounding error
				metricWith("TestResultDouble", 123.456)
		))));
	}

	@Test
	public void testInternalWriteValuePath() throws Exception {
		String namespace = "TestNamespace";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(), false, false,
				namespace, ImmutableList.<Map<String, Object>>of(),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);
		cloudWatchWriter.start();

		Iterable<Result> results = ImmutableList.of(
				result("TestResult1", 123.456d, valuePath()),
				result("TestResult2", 123.456d, valuePath("TestValuePath")),
				result("TestResult3", 123.456d, valuePath("Test", "Value", "Path"))
		);

		cloudWatchWriter.doWrite(server, query, results);

		verify(amazonCloudWatch).putMetricData(argThat(requestWith(namespace, hasMetrics(
				metricWith("TestResult1", 123.456),
				metricWith("TestResult2_TestValuePath", 123.456),
				metricWith("TestResult3_Test.Value.Path", 123.456)
		))));
	}

	@Test
	public void testInternalWriteNoResults() throws Exception {
		String namespace = "TestNamespace";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(), false, false,
				namespace, ImmutableList.<Map<String, Object>>of(),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);
		cloudWatchWriter.start();

		Iterable<Result> results = ImmutableList.of();

		cloudWatchWriter.doWrite(server, query, results);

		verify(amazonCloudWatch).putMetricData(argThat(requestWith(namespace)));
	}

	@Test
	public void testInternalWriteNonNumericResults() throws Exception {
		String namespace = "TestNamespace";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(), false, false,
				namespace, ImmutableList.<Map<String, Object>>of(),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);
		cloudWatchWriter.start();

		Iterable<Result> results = ImmutableList.of(
				// null, // null is not valid, and throws at ImmutableList
				// result("TestResultNull", null), // null is not valid, and throws at Result
				result("TestResultBooleanFalse", false),
				result("TestResultBooleanTrue", true),
				result("TestResultObject", new Object()),
				// result("TestResultStringEmpty", ""), // empty string is valid, but throws at ObjectToDouble
				// result("TestResultStringNumeric", "123.456"), // numeric string is valid, but throws at ObjectToDouble
				result("TestResultStringNonNumeric", "non_numeric")
		);

		cloudWatchWriter.doWrite(server, query, results);

		verify(amazonCloudWatch).putMetricData(argThat(requestWith(namespace)));
	}

	@Test
	public void testInternalWriteStaticDimensions() throws Exception {
		String namespace = "TestNamespace";
		String dimension1Name = "TestDimension1Name";
		String dimension1Value = "TestDimension1Value";
		String dimension2Name = "TestDimension2Name";
		String dimension2Value = "TestDimension2Value";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(), false, false,
				namespace, ImmutableList.of(dimensionMap(dimension1Name, dimension1Value), dimensionMap(dimension2Name, dimension2Value)),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);
		cloudWatchWriter.start();

		Iterable<Result> results = ImmutableList.of(
				result("TestResult1", 123.456d),
				result("TestResult2", 123.456d)
		);

		cloudWatchWriter.doWrite(server, query, results);

		verify(amazonCloudWatch).putMetricData(argThat(requestWith(namespace, hasMetrics(
				metricWith("TestResult1", 123.456, hasDimensions(
						dimensionWith(dimension1Name, dimension1Value),
						dimensionWith(dimension2Name, dimension2Value)
				)),
				metricWith("TestResult2", 123.456, hasDimensions(
						dimensionWith(dimension1Name, dimension1Value),
						dimensionWith(dimension2Name, dimension2Value)
				))
		))));
	}

	@Test
	public void testInternalWriteDynamicDimensions() throws Exception {
		String namespace = "TestNamespace";
		String dimension1Name = "TestDimension1Name";
		String dimension1Value = "TestDimension1Value";
		String dimension2Name = "TestDimension2Name";
		String dimension2Value = "TestDimension2Value";

		cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.of(dimension1Name, dimension2Name), false, false,
				namespace, ImmutableList.<Map<String, Object>>of(),
				ImmutableMap.<String, Object>of()) {
			@Override
			AmazonCloudWatch startAmazonCloudWatch() {
				return amazonCloudWatch;
			}
		};
		cloudWatchWriter.validateSetup(server, query);
		cloudWatchWriter.start();

		Iterable<Result> results = ImmutableList.of(
				result("TestResult1", 123.456d, typeNameValue(dimension1Name, dimension1Value)),
				result("TestResult2", 123.456d, typeNameValue(dimension2Name, dimension2Value)),
				result("TestResult3", 123.456d, typeNameValue(dimension1Name, dimension1Value), typeNameValue(dimension2Name, dimension2Value))
		);

		cloudWatchWriter.doWrite(server, query, results);

		verify(amazonCloudWatch).putMetricData(argThat(requestWith(namespace, hasMetrics(
				metricWith("TestResult1", 123.456, hasDimensions(
						dimensionWith(dimension1Name, dimension1Value)
				)),
				metricWith("TestResult2", 123.456, hasDimensions(
						dimensionWith(dimension2Name, dimension2Value)
				)),
				metricWith("TestResult3", 123.456, hasDimensions(
						dimensionWith(dimension1Name, dimension1Value),
						dimensionWith(dimension2Name, dimension2Value)
				))
		))));
	}

	private Map<String, Object> dimensionMap(String name, String value) {
		return ImmutableMap.<String, Object>of("name", name, "value", value);
	}

	private Dimension dimension(String name, String value) {
		return new Dimension().withName(name).withValue(value);
	}

	private ImmutableList<String> valuePath(String... valuePath) {
		return ImmutableList.copyOf(valuePath);
	}

	private String typeNameValue(String name, String value) {
		return name + "=" + value;
	}

	private Result result(String name, Object value) {
		return result(name, value, valuePath());
	}

	private Result result(String name, Object value, ImmutableList<String> valuePath) {
		return result(name, value, ImmutableList.of(typeNameValue("TestTypeName", "TestTypeValue")), valuePath);
	}

	private Result result(String name, Object value, String... typeNameValues) {
		return result(name, value, ImmutableList.copyOf(typeNameValues), valuePath());
	}

	private Result result(String name, Object value, ImmutableList<String> typeNameValues, ImmutableList<String> valuePath) {
		return new Result(System.currentTimeMillis(), name, "TestClassName", "TestObjDomain", "TestKeyAlias", Joiner.on(",").join(typeNameValues), valuePath, value);
	}

	private ArgumentMatcher<PutMetricDataRequest> requestWith(String namespace) {
		return requestWith(namespace, noMetrics());
	}

	private ArgumentMatcher<PutMetricDataRequest> requestWith(final String namespace, final ArgumentMatcher<List<MetricDatum>> metricsMatcher) {
		return new ArgumentMatcher<PutMetricDataRequest>() {
			@Override
			public boolean matches(PutMetricDataRequest request) {
				return namespace.equals(request.getNamespace()) && metricsMatcher.matches(request.getMetricData());
			}

			@Override
			public String toString() {
				return "{" + "Namespace: " + namespace + "," + "MetricData: " + metricsMatcher.toString() + "}";
			}
		};
	}

	private ArgumentMatcher<List<MetricDatum>> noMetrics() {
		return hasMetrics();
	}

	@SafeVarargs
	private final ArgumentMatcher<List<MetricDatum>> hasMetrics(final ArgumentMatcher<MetricDatum>... metricMatchers) {
		return new ArgumentMatcher<List<MetricDatum>>() {
			@Override
			public boolean matches(List<MetricDatum> metricData) {
				if (metricMatchers.length != metricData.size())
					return false;

				for (int i = 0; i < metricMatchers.length; ++i) {
					if (!metricMatchers[i].matches(metricData.get(i)))
						return false;
				}

				return true;
			}

			@Override
			public String toString() {
				return Arrays.toString(metricMatchers);
			}
		};
	}

	private ArgumentMatcher<MetricDatum> metricWith(String name, double value) {
		return metricWith(name, value, noDimensions());
	}

	private ArgumentMatcher<MetricDatum> metricWith(final String name, final double value, final ArgumentMatcher<List<Dimension>> dimensionsMatcher) {
		return new ArgumentMatcher<MetricDatum>() {
			@Override
			public boolean matches(MetricDatum metricDatum) {
				return name.equals(metricDatum.getMetricName()) && value == metricDatum.getValue() && dimensionsMatcher.matches(metricDatum.getDimensions());
			}

			@Override
			public String toString() {
				return "{" + "MetricName: " + name + "," + "Dimensions: " + dimensionsMatcher.toString() + "," + "Value: " + value + "}";
			}
		};
	}

	private ArgumentMatcher<List<Dimension>> noDimensions() {
		return hasDimensions();
	}

	@SafeVarargs
	private final ArgumentMatcher<List<Dimension>> hasDimensions(final ArgumentMatcher<Dimension>... dimensionMatchers) {
		return new ArgumentMatcher<List<Dimension>>() {
			@Override
			public boolean matches(List<Dimension> dimensions) {
				if (dimensionMatchers.length != dimensions.size())
					return false;

				for (int i = 0; i < dimensionMatchers.length; ++i) {
					if (!dimensionMatchers[i].matches(dimensions.get(i)))
						return false;
				}

				return true;
			}

			@Override
			public String toString() {
				return Arrays.toString(dimensionMatchers);
			}
		};
	}

	private ArgumentMatcher<Dimension> dimensionWith(final String name, final String value) {
		return new ArgumentMatcher<Dimension>() {
			@Override
			public boolean matches(Dimension dimension) {
				return name.equals(dimension.getName()) && value.equals(dimension.getValue());
			}

			@Override
			public String toString() {
				return "{" + "Name: " + name + "," + "Value: " + value + "}";
			}
		};
	}

}
