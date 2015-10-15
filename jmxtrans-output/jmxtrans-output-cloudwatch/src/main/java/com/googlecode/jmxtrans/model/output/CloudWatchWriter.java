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
package com.googlecode.jmxtrans.model.output;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.util.ObjectToDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Writes data to <a href="http://aws.amazon.com/cloudwatch/">AWS CloudWatch</a> using the AWS Java SDK
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
public class CloudWatchWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(CloudWatchWriter.class);

	private AmazonCloudWatchClient cloudWatchClient;
	private String namespace;

	private final ObjectToDouble toDoubleConverter = new ObjectToDouble();

	@JsonCreator
	public CloudWatchWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("namespace") String namespace,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.namespace = MoreObjects.firstNonNull(namespace, (String) getSettings().get("namespace"));
		if (isNullOrEmpty(this.namespace)) throw new IllegalArgumentException("namespace cannot be null or empty");
	}

	@VisibleForTesting
	void setCloudWatchClient(AmazonCloudWatchClient cloudWatchClient) {
		this.cloudWatchClient = cloudWatchClient;
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		if (cloudWatchClient == null) {

			// Configuring the CloudWatch client
			// Credentials are loaded from the Amazon EC2 Instance Metadata Service

			cloudWatchClient = new AmazonCloudWatchClient(new InstanceProfileCredentialsProvider());
			Region awsRegion = Regions.getCurrentRegion();
			if (awsRegion == null) throw new ValidationException("Problems getting metadata", query);
			cloudWatchClient.setRegion(awsRegion);
		}
	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		PutMetricDataRequest metricDataRequest = new PutMetricDataRequest();
		metricDataRequest.setNamespace(namespace);
		List<MetricDatum> metricDatumList = new ArrayList<MetricDatum>();

		// Iterating through the list of query results

		for (Result result : results) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Map.Entry<String, Object> values : resultValues.entrySet()) {
					try {
						metricDatumList.add(processResult(result, values));
					} catch (IllegalArgumentException iae) {
						log.error("Could not convert result to double", iae);
					}
				}
			}
		}

		metricDataRequest.setMetricData(metricDatumList);
		cloudWatchClient.putMetricData(metricDataRequest);
	}

	private MetricDatum processResult(Result result, Map.Entry<String, Object> values) {
		// Sometimes the attribute name and the key of the value are the same
		MetricDatum metricDatum = new MetricDatum();
		if (result.getAttributeName().equals(values.getKey())) {
			metricDatum.setMetricName(result.getAttributeName());
		} else {
			metricDatum.setMetricName(result.getAttributeName() + "_" + values.getKey());
		}

		// Converts the Objects to Double-values for CloudWatch
		metricDatum.setValue(toDoubleConverter.apply(values.getValue()));
		metricDatum.setTimestamp(new Date());
		return metricDatum;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final ImmutableList.Builder<String> typeNames = ImmutableList.builder();
		private boolean booleanAsNumber;
		private Boolean debugEnabled;
		private String namespace;

		private Builder() {
		}

		public Builder addTypeNames(List<String> typeNames) {
			this.typeNames.addAll(typeNames);
			return this;
		}

		public Builder addTypeName(String typeName) {
			typeNames.add(typeName);
			return this;
		}

		public Builder setBooleanAsNumber(boolean booleanAsNumber) {
			this.booleanAsNumber = booleanAsNumber;
			return this;
		}

		public Builder setDebugEnabled(boolean debugEnabled) {
			this.debugEnabled = debugEnabled;
			return this;
		}
		
		public Builder setNamespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

		public CloudWatchWriter build() {
			return new CloudWatchWriter(
					typeNames.build(),
					booleanAsNumber,
					debugEnabled,
					namespace,
					null);
		}

	}
}
