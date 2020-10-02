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

import static com.google.common.base.MoreObjects.firstNonNull;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.util.NumberUtils.isValidNumber;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * {@link com.googlecode.jmxtrans.model.OutputWriter} for
 * <a href="https://influxdb.com/index.html">InfluxDB</a>.
 *
 * @author Simon Hutchinson
 *         <a href="https://github.com/sihutch">github.com/sihutch</a>
 */
@ThreadSafe
public class InfluxDbWriter extends OutputWriterAdapter {
	private static final Logger log = LoggerFactory.getLogger(InfluxDbWriter.class);

	public static final String TAG_HOSTNAME = "hostname";
	public static final String JMX_PORT_KEY = "_jmx_port";

	@Nonnull private final InfluxDB influxDB;
	@Nonnull private final String database;
	@Nonnull private final ConsistencyLevel writeConsistency;
	@Nonnull private final String retentionPolicy;
	@Nonnull private final ImmutableMap<String,String> tags;
	/**
	 * The {@link ImmutableSet} of {@link ResultAttribute} attributes of
	 * {@link Result} that will be written as {@link Point} tags
	 */
	@Nonnull private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;
	@Nonnull ImmutableList<String> typeNames;
	@Nonnull private final ImmutableList<String> attributesAsTags;



	private final boolean createDatabase;
	private final boolean typeNamesAsTags;
	private final boolean allowStringValues;
	private final boolean reportJmxPortAsTag;

	public InfluxDbWriter(
			@Nonnull InfluxDB influxDB,
			@Nonnull String database,
			@Nonnull ConsistencyLevel writeConsistency,
			@Nonnull String retentionPolicy,
			@Nonnull ImmutableMap<String,String> tags,
			@Nonnull ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags,
			@Nonnull ImmutableList<String> typeNames,
			@Nonnull ImmutableList<String> attributesAsTags,
			boolean createDatabase,
			boolean reportJmxPortAsTag,
			boolean typeNamesAsTags,
			boolean allowStringValues) {
		this.influxDB = influxDB;
		this.database = database;
		this.writeConsistency = writeConsistency;
		this.retentionPolicy = retentionPolicy;
		this.tags = tags;
		this.resultAttributesToWriteAsTags = resultAttributesToWriteAsTags;
		this.typeNames = typeNames;
		this.attributesAsTags = attributesAsTags;
		this.createDatabase = createDatabase;
		this.reportJmxPortAsTag = reportJmxPortAsTag;
		this.typeNamesAsTags = typeNamesAsTags;
		this.allowStringValues = allowStringValues;
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
	 * <p>
	 * {@link Server#getPort()} is written as a field, unless {@link #reportJmxPortAsTag} is set to {@code true}
	 * </p>
	 *
	 */
	@Override
	public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {

		// Creates only if it doesn't already exist
		if (createDatabase) influxDB.createDatabase(database);
		BatchPoints.Builder batchPointsBuilder = BatchPoints.database(database).retentionPolicy(retentionPolicy)
				.tag(TAG_HOSTNAME, server.getSource());

		//Custom tags
		for(Map.Entry<String,String> tag : tags.entrySet()) {
			batchPointsBuilder.tag(tag.getKey(),tag.getValue());
		}
		
		ImmutableList<String> typeNamesParam = null;
		// if not typeNamesAsTag, we concat typeName in values.
		if (!typeNamesAsTags) {
			typeNamesParam = this.typeNames;
		}
		

		Map<String,Map<String, String>> attrTagByTypeName = buildAttrTagByTypeName(query, results, typeNamesParam);

		BatchPoints batchPoints = batchPointsBuilder.consistency(writeConsistency).build();
		
		for (Result result : results) {
			log.debug("Query result: {}", result);

			HashMap<String, Object> fieldValues = newHashMap();
			
			Object value = result.getValue();

			String key = KeyUtils.getPrefixedKeyString(query, result, typeNamesParam);
			if (isValidValue(value) && !attributesAsTags.contains(result.getAttributeName())) {
					fieldValues.put(key, value);
			}

			// send the point if filteredValues isn't empty
			if (!fieldValues.isEmpty()) {
				Map<String, String> resultTagsToApply = buildResultTagMap(result);
				Map<String, String> tagValues = firstNonNull(attrTagByTypeName.get(result.getTypeName()),
																	new HashMap<String, String>());
				if (reportJmxPortAsTag) {
					resultTagsToApply.put(JMX_PORT_KEY, server.getPort());
				} else {
					fieldValues.put(JMX_PORT_KEY, Integer.parseInt(server.getPort()));
				}
				Point point = Point.measurement(result.getKeyAlias()).time(result.getEpoch(), MILLISECONDS)
						.tag(resultTagsToApply).tag(tagValues).fields(fieldValues).build();

				log.debug("Point: {}", point);
				batchPoints.point(point);
			}
		}

		influxDB.write(batchPoints);
	}

	private Map<String, String> buildResultTagMap(Result result) {

		Map<String, String> resultTagMap = new TreeMap<>();
		for (ResultAttribute resultAttribute : resultAttributesToWriteAsTags) {
			resultAttribute.addTo(resultTagMap, result);
		}

		if (typeNamesAsTags) {
			Map<String, String> typeNameValueMap = result.getTypeNameMap();
			for (String typeToTag : this.typeNames) {
				if (typeNameValueMap.containsKey(typeToTag)) {
					resultTagMap.put(typeToTag, typeNameValueMap.get(typeToTag));
				}
			}
		}

		return resultTagMap;

	}
	
	/**
	 * Process the "attributesAsTags" parameter.<br>
	 * Creates a tag map, associated by the typeName.
	 * Results fields with the same typeName will receive the same tag values.
	 * @param query
	 * @param results
	 * @param typeNames
	 * @return a map built as follows :
	 * 	<ul>
	 * 	<li>key : the typeName, in order to link tags to the corresponding fields in InfluxDB</li>
	 * 	<li>values : a key-value map of every attributes converted into tags, ready for the InlfuxDB query</li>
	 * 	</ul>
	 */
	private Map<String,Map<String, String>> buildAttrTagByTypeName(Query query, Iterable<Result> results, List<String> typeNames){
		Map<String,Map<String, String>> attrTagByTypeName = newHashMap();
		
		if (!attributesAsTags.isEmpty()) {
			for (Result result : results) {
				if (attributesAsTags.contains(result.getAttributeName())){
					if(!attrTagByTypeName.containsKey(result.getTypeName()))
						attrTagByTypeName.put(result.getTypeName(), new HashMap<String, String>());
					
					String key = KeyUtils.getPrefixedKeyString(query, result, typeNames);
					attrTagByTypeName.get(result.getTypeName())
						.put(key, String.valueOf(result.getValue()));
				}
			}
		}
		return attrTagByTypeName;
	}
	
	private boolean isValidValue(Object value) {
		return isValidNumber(value) || allowStringValues && value instanceof String;
	}

}
