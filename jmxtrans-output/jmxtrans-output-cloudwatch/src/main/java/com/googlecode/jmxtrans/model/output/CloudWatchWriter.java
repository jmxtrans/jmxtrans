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

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import com.googlecode.jmxtrans.util.ObjectToDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Writes data to <a href="http://aws.amazon.com/cloudwatch/">AWS CloudWatch</a> using the AWS Java SDK
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
public class CloudWatchWriter extends BaseOutputWriter {

	private static final Logger logger = LoggerFactory.getLogger(CloudWatchWriter.class);

	private static final String SETTING_NAMESPACE = "namespace";
	private static final String SETTING_DIMENSIONS = "dimensions";

	private static final MapEntryToDimension MAP_ENTRY_TO_DIMENSION = new MapEntryToDimension();
	private static final ObjectToDouble OBJECT_TO_DOUBLE = new ObjectToDouble();

	private final String namespace;
	private final Collection<Map<String, Object>> dimensionsMap;

	private Collection<Dimension> dimensions;
	private AmazonCloudWatch amazonCloudWatch;

	@JsonCreator
	public CloudWatchWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("namespace") String namespace,
			@JsonProperty("dimensions") Collection<Map<String, Object>> dimensions,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);

		this.namespace = MoreObjects.firstNonNull(
				namespace,
				(String) getSettings().get(SETTING_NAMESPACE));
		//noinspection unchecked
		this.dimensionsMap = firstNonNull(
				dimensions,
				(Collection<Map<String, Object>>) getSettings().get(SETTING_DIMENSIONS),
				ImmutableList.<Map<String, Object>>of());
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		if (namespace.isEmpty()) {
			throw new ValidationException("namespace cannot be empty", query);
		}

		try {
			dimensions = toDimensions(dimensionsMap);
		} catch (IllegalArgumentException e) {
			throw new ValidationException("dimensions cannot be incomplete", query, e);
		}
	}

	private ImmutableList<Dimension> toDimensions(Collection<Map<String, Object>> dimensionsMap) throws IllegalArgumentException {
		return FluentIterable.from(dimensionsMap)
				.transform(MAP_ENTRY_TO_DIMENSION)
				.toList();
	}

	@Override
	public void start() throws LifecycleException {
		super.start();

		amazonCloudWatch = startAmazonCloudWatch();
		logger.debug("Started CloudWatch client: {}", amazonCloudWatch);
	}

	AmazonCloudWatch startAmazonCloudWatch() {
		return AmazonCloudWatchClient.builder()
				.withCredentials(InstanceProfileCredentialsProvider.getInstance())
				.withRegion(Regions.getCurrentRegion().getName())
				.build();
	}

	@Override
	public void close() throws LifecycleException {
		if (amazonCloudWatch != null) {
			amazonCloudWatch.shutdown();
			logger.debug("Stopped CloudWatch client: {}", amazonCloudWatch);
		}

		super.close();
	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) {
		PutMetricDataRequest request = new PutMetricDataRequest()
				.withNamespace(namespace)
				.withMetricData(toMetrics(results));
		logger.debug("CloudWatch - Request: {}", request);

		PutMetricDataResult response = amazonCloudWatch.putMetricData(request);
		logger.debug("CloudWatch - Response: {}", response);
	}

	private ImmutableList<MetricDatum> toMetrics(ImmutableList<Result> results) {
		return FluentIterable.from(results)
				.filter(new Predicate<Result>() {
					@Override
					public boolean apply(Result result) {
						return isNumeric(result.getValue());
					}
				})
				.transform(new Function<Result, MetricDatum>() {
					@Override
					public MetricDatum apply(Result result) {
						return toMetric(result);
					}
				})
				.toList();
	}

	private boolean isNumeric(Object value) {
		return NumberUtils.isNumeric(value);
	}

	private MetricDatum toMetric(Result result) {
		ImmutableList<String> typeNames = getTypeNames();
		Map<String, String> typeNameValues = result.getTypeNameMap();

		ImmutableList<Dimension> dimensions = FluentIterable.from(this.dimensions)
				.append(toDimensions(typeNames, typeNameValues))
				.toList();

		String metricName = result.getValuePath().isEmpty() ? result.getAttributeName()
		    	                                   	        : result.getAttributeName() + "_" + KeyUtils.getValuePathString(result);

		return new MetricDatum()
				.withDimensions(dimensions)
				.withMetricName(metricName)
				.withTimestamp(new Date(result.getEpoch()))
				.withValue(toDouble(result.getValue()));
	}

	private ImmutableList<Dimension> toDimensions(ImmutableList<String> typeNames, final Map<String, String> typeNameValues) {
		return FluentIterable.from(typeNames)
				.filter(new Predicate<String>() {
					@Override
					public boolean apply(String typeName) {
						return typeNameValues.containsKey(typeName);
					}
				})
				.transform(new Function<String, Dimension>() {
					@Override
					public Dimension apply(String typeName) {
						return new Dimension().withName(typeName).withValue(typeNameValues.get(typeName));
					}
				})
				.toList();
	}

	private Double toDouble(Object value) {
		return OBJECT_TO_DOUBLE.apply(value);
	}

	String getNamespace() {
		return namespace;
	}

	Collection<Dimension> getDimensions() {
		return dimensions;
	}

}
