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

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

/**
 * {@link com.googlecode.jmxtrans.model.OutputWriter} for
 * <a href="https://influxdb.com/index.html">InfluxDB</a>.
 *
 * @author Simon Hutchinson <https://github.com/sihutch>
 */
public class InfluxDbWriter extends BaseOutputWriter {

	/**
	 * Enumerates the members of {@link Result} that may be written as
	 * {@link Point} tags
	 * 
	 * @author Simon Hutchinson <https://github.com/sihutch>
	 *
	 */
	public enum ResultAttribute {

		TYPENAME("typeName"), OBJDOMAIN("objDomain"), CLASSNAME("className"), ATTRIBUTENAME("attributeName");

		private String tagName;
		private String accessorMethod;

		ResultAttribute(String tagName) {
			this.tagName = tagName;
			this.accessorMethod = "get" + StringUtils.capitalize(tagName);
		}

		public String getTagName() {
			return tagName;
		}

		public String getAccessorMethod() {
			return accessorMethod;
		}
	}

	/**
	 * Setting name that allows attributes from {@link Result} limited by
	 * {@link ResultAttribute} values to be written as {@link Point} tags
	 */
	public static final String SETTING_RESULT_TAGS = "resultTags";

	/**
	 * The {@link EnumSet} of {@link ResultAttribute} attributes of
	 * {@link Result} that will be written as {@link Point} tags
	 */
	private EnumSet<ResultAttribute> resultAttributesToWriteAsTags = EnumSet.allOf(ResultAttribute.class);

	/**
	 * The names of the tag written to every {@link Point} that will contain the
	 * value of {@link Server#getHost()}
	 */
	public static final String TAG_HOSTNAME = "hostname";

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(InfluxDbWriter.class);

	/**
	 * <p>
	 * The name of the setting that can be used to control the write consistency
	 * of each {@link BatchPoints} sent to InfluxDB. Allowed values are:
	 * </p>
	 * 
	 * <ul>
	 * <li>ALL = Write succeeds only if write reached all cluster members.</li>
	 * <li>ANY = Write succeeds if write reached any cluster members.</li>
	 * <li>ONE = Write succeeds if write reached at least one cluster members.
	 * </li>
	 * <li>QUORUM = Write succeeds only if write reached a quorum of cluster
	 * members.</li>
	 * </ul>
	 *
	 */
	public static final String SETTING_WRITE_CONSISTENCY = "writeConsistency";

	/**
	 * <p>
	 * The default value of write consistency for each measurement where no
	 * writeConsistency setting is provided in the json config.
	 * </p>
	 * ALL = Write succeeds only if write reached all cluster members.
	 */
	private static final String DEFAULT_WRITE_CONSISTENCY = "ALL";

	/**
	 * 
	 * The name of the setting that can be used to control the <a href=
	 * "https://influxdb.com/docs/v0.9/concepts/key_concepts.html#retention-policy">
	 * The retention policy</a> for the measurement
	 */
	public static final String SETTING_RETENTION_POLICY = "retentionPolicy";

	/**
	 * The deault <a href=
	 * "https://influxdb.com/docs/v0.9/concepts/key_concepts.html#retention-policy">
	 * The retention policy</a> for each measuremen where no retentionPolicy
	 * setting is provided in the json config
	 */
	private static final String DEFAULT_RETENTION_POLICY = "default";

	private String database;
	private ConsistencyLevel writeConsistency = ConsistencyLevel.ALL;
	private String retentionPolicy;

	/** Thread safe **/
	InfluxDB influxDB;

	/**
	 * @param typeNames
	 * @param booleanAsNumber
	 * @param debugEnabled
	 * @param url
	 *            - The url e.g http://localhost:8086 to InfluxDB
	 * @param username
	 *            - The username for InfluxDB
	 * @param password
	 *            - The password for InfluxDB
	 * @param database
	 *            - The name of the database (created if does not exist) on
	 *            InfluxDB to write the measurements to
	 * @param settings
	 */
	@JsonCreator
	public InfluxDbWriter(@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber, @JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("url") String url, @JsonProperty("username") String username,
			@JsonProperty("password") String password, @JsonProperty("database") String database,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);

		this.database = database;

		initWriteConsistency(settings);
		initRetentionPolicy(settings);
		initResultAttributesToWriteAsTags(settings);

		LOG.debug("Connecting to url: {} as: username: {}", url, username);

		influxDB = InfluxDBFactory.connect(url, username, password);
	}

	private void initWriteConsistency(Map<String, Object> settings) {
		String consistencySetting = Settings
				.getStringSetting(settings, SETTING_WRITE_CONSISTENCY, DEFAULT_WRITE_CONSISTENCY).toUpperCase();
		writeConsistency = ConsistencyLevel.valueOf(consistencySetting);
		LOG.debug("Write consistency set to: {}", writeConsistency);
	}

	private void initRetentionPolicy(Map<String, Object> settings) {
		retentionPolicy = Settings.getStringSetting(settings, SETTING_RETENTION_POLICY, DEFAULT_RETENTION_POLICY);
		LOG.debug("Retention Policy set to: {}", retentionPolicy);
	}
	
	private void initResultAttributesToWriteAsTags(Map<String, Object> settings) {
		@SuppressWarnings("unchecked")
		List<String> resultTagsFromSettings = (List<String>) settings.get(SETTING_RESULT_TAGS);
		if (resultTagsFromSettings != null) {
			resultAttributesToWriteAsTags.clear();
			for (String resultTag : resultTagsFromSettings) {
				resultAttributesToWriteAsTags.add(ResultAttribute.valueOf(resultTag.toUpperCase()));
			}
		}
		LOG.debug("Result Tags to write set to: {}", resultAttributesToWriteAsTags);
	}

	@VisibleForTesting
	void setInfluxDB(InfluxDB influxDB) {
		this.influxDB = influxDB;
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		// Not implemented
	}

	/**
	 * <p>
	 * Each {@link Result} is written as a {@link Point} to InfluxDB
	 * </p>
	 * 
	 * <p>
	 * The measurement for the {@link Point} is to {@link Result#getKeyAlias()}
	 * <p>
	 * <a href=
	 * "https://influxdb.com/docs/v0.9/concepts/key_concepts.html#retention-policy">
	 * The retention policy</a> for the measurement is set to "default" unless
	 * overridden in settings:
	 * </p>
	 * 
	 * <p>
	 * The write consistency level defaults to "ALL" unless overridden in
	 * settings:
	 * 
	 * <ul>
	 * <li>ALL = Write succeeds only if write reached all cluster members.</li>
	 * <li>ANY = Write succeeds if write reached any cluster members.</li>
	 * <li>ONE = Write succeeds if write reached at least one cluster members.
	 * </li>
	 * <li>QUORUM = Write succeeds only if write reached a quorum of cluster
	 * members.</li>
	 * </ul>
	 * 
	 * <p>
	 * The time key for the {@link Point} is set to {@link Result#getEpoch()}
	 * </p>
	 * 
	 * <p>
	 * All {@link Result#getValues()} are written as fields to the {@link Point}
	 * </p>
	 * 
	 * <p>
	 * The following properties from {@link Result} are written as tags to the
	 * {@link Point} unless overriden in settings:
	 * 
	 * <ul>
	 * <li>{@link Result#getAttributeName()}</li>
	 * <li>{@link Result#getClassName()}</li>
	 * <li>{@link Result#getObjDomain()}</li>
	 * <li>{@link Result#getTypeName()}</li>
	 * </ul>
	 * <p>
	 * {@link Server#getHost()} is set as a tag on every {@link Point}
	 * </p>
	 * 
	 */
	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		// Creates only if it doesn't already exist
		influxDB.createDatabase(database);

		BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy(retentionPolicy)
				.tag(TAG_HOSTNAME, server.getHost()).consistency(writeConsistency).build();
		for (Result result : results) {
			Map<String, String> resultTagsToApply = buildResultTagMap(result);
			Point point = Point.measurement(result.getKeyAlias()).time(result.getEpoch(), TimeUnit.MILLISECONDS)
					.tag(resultTagsToApply).fields(result.getValues()).build();
			batchPoints.point(point);
		}
		influxDB.write(batchPoints);
	}
	
	/**
	 * Adds data from {@link Result} to a map based on the attributes configured in <code>resultAttributesToWriteAsTags</code>
	 * @param result The {@link Result} to get the data from 
	 * @return A map based on the attributes configured in <code>resultAttributesToWriteAsTags</code>
	 * @throws Exception If refection cannot be performed on the {@link Result}
	 */
	private Map<String, String> buildResultTagMap(Result result) throws Exception {
		Map<String, String> resultTagsToApply = new TreeMap<String, String>();
		for (ResultAttribute resultAttribute : resultAttributesToWriteAsTags) {
			Method m = result.getClass().getMethod(resultAttribute.getAccessorMethod());
			resultTagsToApply.put(resultAttribute.getTagName(), (String) m.invoke(result));
		}
		return resultTagsToApply;
	}
}