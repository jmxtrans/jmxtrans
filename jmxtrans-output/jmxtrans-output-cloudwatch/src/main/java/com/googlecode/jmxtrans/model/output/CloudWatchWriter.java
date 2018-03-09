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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.util.ObjectToDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Writes data to <a href="http://aws.amazon.com/cloudwatch/">AWS CloudWatch</a> using the AWS Java SDK
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
public class CloudWatchWriter implements OutputWriterFactory {
	private static final Logger log = LoggerFactory.getLogger(CloudWatchWriter.class);
	public static final MapEntryToDimension MAP_ENTRY_TO_DIMENSION = new MapEntryToDimension();

	private final String namespace;
	private final Iterable<Map<String, Object>> dimensions;

	private final boolean booleanAsNumber;

	@JsonCreator
	public CloudWatchWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("namespace") String namespace,
			@JsonProperty("dimensions") Collection<Map<String,Object>> dimensions,
			@JsonProperty("settings") Map<String, Object> settings) {
		this.booleanAsNumber = booleanAsNumber;
		this.namespace = firstNonNull(namespace, (String) settings.get("namespace"));
		checkArgument(!isNullOrEmpty(this.namespace), "namespace cannot be null or empty");

		this.dimensions = firstNonNull(dimensions, (Collection<Map<String, Object>>) settings.get("dimensions"));
	}

	/**
	 * Configuring the CloudWatch client.
	 *
	 * Credentials are loaded from the Amazon EC2 Instance Metadata Service
	 */
	private AmazonCloudWatchClient createCloudWatchClient() {
		AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(new InstanceProfileCredentialsProvider());
		cloudWatchClient.setRegion(checkNotNull(Regions.getCurrentRegion(), "Problems getting AWS metadata"));
		return cloudWatchClient;
	}

	private ImmutableList<Dimension> createDimensions(Iterable<Map<String, Object>> dimensions) {
		if (dimensions == null) return ImmutableList.of();

		return FluentIterable.from(dimensions).transform(MAP_ENTRY_TO_DIMENSION).toList();
	}

	@Override
	public OutputWriter create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				new Writer(namespace, createCloudWatchClient(), createDimensions(dimensions))
		);
	}

	public static class Writer extends OutputWriterAdapter {

		@Nonnull private final String namespace;
		@Nonnull private final AmazonCloudWatch cloudWatchClient;

		@Nonnull private final ObjectToDouble toDoubleConverter = new ObjectToDouble();
		@Nonnull private final ImmutableCollection<Dimension> dimensions;

		public Writer(@Nonnull String namespace, @Nonnull AmazonCloudWatch cloudWatchClient, @Nonnull ImmutableCollection<Dimension> dimensions) {
			this.namespace = namespace;
			this.cloudWatchClient = cloudWatchClient;
			this.dimensions = dimensions;
		}

		@Override
		public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
			PutMetricDataRequest metricDataRequest = new PutMetricDataRequest();
			metricDataRequest.setNamespace(namespace);
			List<MetricDatum> metricDatumList = new ArrayList<>();

			// Iterating through the list of query results

			for (Result result : results) {
				try {
					metricDatumList.add(processResult(result));
				} catch (IllegalArgumentException iae) {
					log.error("Could not convert result to double", iae);
				}
			}

			metricDataRequest.setMetricData(metricDatumList);
			cloudWatchClient.putMetricData(metricDataRequest);
		}

		private MetricDatum processResult(Result result) {
			// Sometimes the attribute name and the key of the value are the same
			MetricDatum metricDatum = new MetricDatum();
			if (result.getValuePath().isEmpty()) {
				metricDatum.setMetricName(result.getAttributeName());
			} else {
				metricDatum.setMetricName(result.getAttributeName() + "_" + KeyUtils.getValuePathString(result));
			}

			metricDatum.setDimensions(dimensions);

			// Converts the Objects to Double-values for CloudWatch
			metricDatum.setValue(toDoubleConverter.apply(result.getValue()));
			metricDatum.setTimestamp(new Date());
			return metricDatum;
		}

		@Override
		public void close() throws LifecycleException {
			cloudWatchClient.shutdown();
		}
	}

}
