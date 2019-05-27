package com.googlecode.jmxtrans.model.output;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author Rimal
 */
public class CloudWatchWriterFactory implements OutputWriterFactory {

		public static final MapEntryToDimension MAP_ENTRY_TO_DIMENSION = new MapEntryToDimension();

		private final String namespace;
		private final ImmutableList<String> typeNames;
		private final Iterable<Map<String, Object>> dimensions;

		private final boolean booleanAsNumber;

		@JsonCreator
		public CloudWatchWriterFactory(
				@JsonProperty("typeNames") ImmutableList<String> typeNames,
				@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
				@JsonProperty("debug") Boolean debugEnabled,
				@JsonProperty("namespace") String namespace,
				@JsonProperty("dimensions") Collection<Map<String, Object>> dimensions,
				@JsonProperty("settings") Map<String, Object> settings) {
				this.booleanAsNumber = booleanAsNumber;
				this.namespace = firstNonNull(namespace, (String) settings.get("namespace"));
				checkArgument(!isNullOrEmpty(this.namespace), "namespace cannot be null or empty");

				this.typeNames = typeNames;
				this.dimensions = firstNonNull(dimensions, (Collection<Map<String, Object>>) settings.get("dimensions"));
		}

		/**
		 * Configuring the CloudWatch client.
		 * <p>
		 * Credentials are loaded from the Amazon EC2 Instance Metadata Service
		 */
		private AmazonCloudWatchClient createCloudWatchClient() {
				AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(new InstanceProfileCredentialsProvider());
				cloudWatchClient.setRegion(checkNotNull(Regions.getCurrentRegion(), "Problems getting AWS metadata"));
				return cloudWatchClient;
		}

		private ImmutableList<Dimension> createDimensions(Iterable<Map<String, Object>> dimensions) {
				if (dimensions == null) {
						return ImmutableList.of();
				}

				return FluentIterable.from(dimensions).transform(MAP_ENTRY_TO_DIMENSION).toList();
		}

		@Nonnull
		@Override
		public ResultTransformerOutputWriter<CloudWatchWriter> create() {
				return ResultTransformerOutputWriter.booleanToNumber(
						booleanAsNumber,
						new CloudWatchWriter(namespace, createCloudWatchClient(), createDimensions(dimensions), typeNames)
				);
		}
}
