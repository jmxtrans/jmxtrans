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
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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

	public static final String METADATA_URL = "http://169.254.169.254/latest/dynamic/instance-identity/document";
	public static final String REGION = "region";
	public static final String ENCODING = "UTF-8";

	@JsonCreator
	public CloudWatchWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("useObjDomain") Boolean useObjDomain,
			@JsonProperty("namespace") String namespace,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, useObjDomain, settings);
		this.namespace = MoreObjects.firstNonNull(namespace, (String) getSettings().get("namespace"));
		if (isNullOrEmpty(this.namespace)) throw new IllegalArgumentException("namespace cannot be null or empty");
	}

	/**
	 * Determines the region of the EC2-instance by parsing the instance metadata.
	 * For more information on instance metadata take a look at <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AESDG-chapter-instancedata.html">Instance Metadata and User Data</a></>
	 *
	 * @return The region as String
	 * @throws IOException If the instance meta-data is not available
	 */
	@Nonnull
	private String getRegion() throws IOException {
		Closer closer = Closer.create();
		try {
			// URL of the instance metadata service
			URL url = new URL(METADATA_URL);
			URLConnection conn = url.openConnection();

			BufferedReader in = closer.register(
					new BufferedReader(
						new InputStreamReader(
							conn.getInputStream(), ENCODING)));

			return parseRegion(in);
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@Nonnull
	@VisibleForTesting
	String parseRegion(BufferedReader in) throws IOException {
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			if (inputLine.contains(REGION)) {
				String[] splitLine = inputLine.split(":");
				return splitLine[1].replaceAll("\"", "").replaceAll(",", "").trim();
			}
		}
		throw new IllegalArgumentException("No valid region found");
	}

	/**
	 * Converts Objects to Doubles for CloudWatch
	 *
	 * @param obj The object to convert
	 * @return The Double-value
	 */
	@Nullable
	@VisibleForTesting
	Double convertToDouble(Object obj) {
		if (obj instanceof Double) return (Double) obj;
		if (obj instanceof Number) return ((Number) obj).doubleValue();

		log.error("There is no converter from " + obj.getClass().getName() + " to Double ");
		return null;
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		try {
			if (cloudWatchClient == null) {

				// Configuring the CloudWatch client
				// Credentials are loaded from the Amazon EC2 Instance Metadata Service

				cloudWatchClient = new AmazonCloudWatchClient(new InstanceProfileCredentialsProvider());
				Region awsRegion = Region.getRegion(Regions.fromName(getRegion()));
				cloudWatchClient.setRegion(awsRegion);
			}
		} catch (IOException exc) {
			throw new ValidationException("Problems getting metadata", query);
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

					// Sometimes the attribute name and the key of the value are the same

					MetricDatum metricDatum = new MetricDatum();
					if (result.getAttributeName().equals(values.getKey())) {
						metricDatum.setMetricName(result.getAttributeName());
					} else {
						metricDatum.setMetricName(result.getAttributeName() + "_" + values.getKey());
					}

					// Converts the Objects to Double-values for CloudWatch

					metricDatum.setValue(convertToDouble(values.getValue()));
					metricDatum.setTimestamp(new Date());

					metricDatumList.add(metricDatum);
				}
			}
		}

		metricDataRequest.setMetricData(metricDatumList);
		cloudWatchClient.putMetricData(metricDataRequest);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final ImmutableList.Builder<String> typeNames = ImmutableList.builder();
		private boolean booleanAsNumber;
		private Boolean debugEnabled;
		private Boolean useObjDomain;
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
		
		public Builder setUseObjDomain(boolean useObjDomain) {
			this.useObjDomain = useObjDomain;
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
					useObjDomain,
					namespace,
					null);
		}

	}
}
