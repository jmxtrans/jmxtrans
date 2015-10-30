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

import java.util.Map;
import java.util.concurrent.TimeUnit;

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

	public static final String TAG_TYPE_NAME = "typeName";
	public static final String TAG_OBJ_DOMAIN = "objDomain";
	public static final String TAG_CLASS_NAME = "className";
	public static final String TAG_ATTRIBUTE_NAME = "attributeName";
	public static final String TAG_HOSTNAME = "hostname";
	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(InfluxDbWriter.class);
	public static final String SETTING_WRITE_CONSISTENCY = "writeConsistency";
	private static final String DEFAULT_WRITE_CONSISTENCY = "ALL";

	private static final String SETTING_RETENTION_POLICY = "retentionPolicy";
	private static final String DEFAULT_RETENTION_POLICY = "default";

	private String database;
	private ConsistencyLevel writeConsistency = ConsistencyLevel.ALL;
	private String retentionPolicy;

	/** Thread safe **/
	InfluxDB influxDB;

	@JsonCreator
	public InfluxDbWriter(@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber, @JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("url") String url, @JsonProperty("username") String username,
			@JsonProperty("password") String password, @JsonProperty("database") String database,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);

		this.database = database;

		String consistencySetting = Settings
				.getStringSetting(settings, SETTING_WRITE_CONSISTENCY, DEFAULT_WRITE_CONSISTENCY).toUpperCase();
		writeConsistency = ConsistencyLevel.valueOf(consistencySetting);
		retentionPolicy = Settings.getStringSetting(settings, SETTING_RETENTION_POLICY, DEFAULT_RETENTION_POLICY);
		LOG.debug("Connecting to url: {} as: username: {}", url, username);

		influxDB = InfluxDBFactory.connect(url, username, password);
	}

	@VisibleForTesting
	void setInfluxDB(InfluxDB influxDB) {
		this.influxDB = influxDB;
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		// TODO Auto-generated method stub
	}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) {
		// Creates only if it doesn't already exist
		influxDB.createDatabase(database);

		BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy(retentionPolicy)
				.tag(TAG_HOSTNAME, server.getHost()).consistency(writeConsistency).build();
		Point point;
		for (Result result : results) {
			point = Point.measurement(result.getKeyAlias()).time(result.getEpoch(), TimeUnit.MILLISECONDS)
					.tag(TAG_ATTRIBUTE_NAME, result.getAttributeName()).tag(TAG_CLASS_NAME, result.getClassName())
					.tag(TAG_OBJ_DOMAIN, result.getObjDomain()).tag(TAG_TYPE_NAME, result.getTypeName())
					.fields(result.getValues()).build();

			batchPoints.point(point);
		}
		influxDB.write(batchPoints);
	}
}