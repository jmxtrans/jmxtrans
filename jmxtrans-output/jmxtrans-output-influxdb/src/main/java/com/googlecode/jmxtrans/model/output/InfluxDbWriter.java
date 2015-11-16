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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.Server;

/**
 * {@link com.googlecode.jmxtrans.model.OutputWriter} for
 * <a href="https://influxdb.com/index.html">InfluxDB</a>.
 *
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 */
@ThreadSafe
public class InfluxDbWriter extends OutputWriterAdapter {

	public static final String TAG_HOSTNAME = "hostname";

	@Nonnull private final InfluxDB influxDB;
	@Nonnull private final String database;
	@Nonnull private final ConsistencyLevel writeConsistency;
	@Nonnull private final String retentionPolicy;

	/**
	 * The {@link ImmutableSet} of {@link ResultAttribute} attributes of
	 * {@link Result} that will be written as {@link Point} tags
	 */
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;

	// Logging
	private static final Logger log = LoggerFactory.getLogger(InfluxDbWriter.class);

	public InfluxDbWriter(
			@Nonnull InfluxDB influxDB,
			@Nonnull String database,
			@Nonnull ConsistencyLevel writeConsistency,
			@Nonnull String retentionPolicy,
			@Nonnull ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags) {
		this.database = database;
		this.writeConsistency = writeConsistency;
		this.retentionPolicy = retentionPolicy;
		this.influxDB = influxDB;
		this.resultAttributesToWriteAsTags = resultAttributesToWriteAsTags;
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
	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		// Creates only if it doesn't already exist
		influxDB.createDatabase(database);

		// use alias if provided, otherwise hostname
		String source = getSource(server);

		BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy(retentionPolicy)
				.tag(TAG_HOSTNAME, source).consistency(writeConsistency).build();

		for (Result result : results) {
			// we'll create a copy of result values here
			Map<String, Object> fixedValues = new HashMap<String,Object>();
			// _jmx_port as a field so we can filter on it in influx
			fixedValues.put("_jmx_port", Integer.parseInt(server.getPort()));
			// clean up an NaN values in values
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Entry<String, Object> entry : resultValues.entrySet()) {
					// we want to ignore NaN's
					if (!entry.getValue().toString().equals("NaN")) {
						fixedValues.put(entry.getKey(), entry.getValue());
					}
				}
			}
			// send the point if fixedValues isn't empty
			if (fixedValues.size() > 1) {
				Map<String, String> resultTagsToApply = buildResultTagMap(result);
				Point point = Point.measurement(result.getKeyAlias()).time(result.getEpoch(), MILLISECONDS)
						.tag(resultTagsToApply).fields(fixedValues).build();
				batchPoints.point(point);
			}
		}
		influxDB.write(batchPoints);
	}

	private Map<String, String> buildResultTagMap(Result result) throws Exception {
		Map<String, String> resultTagMap = new TreeMap<String, String>();
		for (ResultAttribute resultAttribute : resultAttributesToWriteAsTags) {
			resultAttribute.addAttribute(resultTagMap, result);
		}
		return resultTagMap;
	}

	private String getSource(Server server) {
		if (server.getAlias() != null) {
			return server.getAlias();
		} else {
			return server.getHost();
		}
	}
}
