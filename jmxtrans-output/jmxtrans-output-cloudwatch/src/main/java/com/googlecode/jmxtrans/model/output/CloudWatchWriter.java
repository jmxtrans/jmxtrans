/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.ObjectToDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Writes data to <a href="http://aws.amazon.com/cloudwatch/">AWS CloudWatch</a> using the AWS Java SDK
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
public class CloudWatchWriter extends OutputWriterAdapter {
		private static final Logger log = LoggerFactory.getLogger(CloudWatchWriter.class);

		@Nonnull
		private final String namespace;
		@Nonnull
		private final AmazonCloudWatch cloudWatchClient;

		@Nonnull
		private final ObjectToDouble toDoubleConverter = new ObjectToDouble();
		@Nonnull
		private final ImmutableCollection<Dimension> dimensions;
		private final ImmutableCollection<String> typeNames;

		public CloudWatchWriter(@Nonnull String namespace,
														@Nonnull AmazonCloudWatch cloudWatchClient,
														@Nonnull ImmutableCollection<Dimension> dimensions,
														ImmutableCollection<String> typeNames
		) {
				this.namespace = namespace;
				this.cloudWatchClient = cloudWatchClient;
				this.dimensions = dimensions;
				this.typeNames = typeNames;
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

				metricDatum.setDimensions(FluentIterable.from(this.dimensions)
						.append(convertTypeNamesToDimensions(result.getTypeNameMap()))
						.toList());

				// Converts the Objects to Double-values for CloudWatch
				metricDatum.setValue(toDoubleConverter.apply(result.getValue()));
				metricDatum.setTimestamp(new Date());
				return metricDatum;
		}

		private ImmutableList<Dimension> convertTypeNamesToDimensions(final Map<String, String> typeNameMap) {
				if (null != typeNameMap && typeNameMap.size() > 0) {
						return FluentIterable.from(typeNames)
								.filter(new Predicate<String>() {
										@Override
										public boolean apply(String typeName) {
												return typeNameMap.containsKey(typeName);
										}
								})
								.transform(new Function<String, Dimension>() {
										@Override
										public Dimension apply(String typeName) {
												return new Dimension().withName(typeName).withValue(typeNameMap.get(typeName));
										}
								})
								.toList();
				} else {
						return ImmutableList.of();
				}
		}

		@Override
		public void close() throws LifecycleException {
				cloudWatchClient.shutdown();
		}
}
