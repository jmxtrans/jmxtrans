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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.ResultAttributes;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import org.apache.commons.lang.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class InfluxDbWriterFactory implements OutputWriterFactory {

	private static final Logger LOG = LoggerFactory.getLogger(InfluxDbWriterFactory.class);

	/**
	 * The deault <a href=
	 * "https://influxdb.com/docs/v1.0/concepts/key_concepts.html#retention-policy">
	 * The retention policy</a> for each measuremen where no retentionPolicy
	 * setting is provided in the json config
	 */
	private static final String DEFAULT_RETENTION_POLICY = "autogen";

	private final String database;
	private final InfluxDB.ConsistencyLevel writeConsistency;
	private final ImmutableMap<String, String> tags;
	private final String retentionPolicy;
	private final InfluxDB influxDB;
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;
	private final boolean booleanAsNumber;
	private final boolean typeNamesAsTags;
	private final boolean createDatabase;
	private final boolean reportJmxPortAsTag;
	private final ImmutableList<String> typeNames;
	private final boolean allowStringValues;

	/**
	 * @param typeNames			- List of typeNames keys to use in fields by default
	 * @param BooleanAsNumber	- output boolean attributes as number
	 * @param url				- The url e.g http://localhost:8086 to InfluxDB
	 * @param username			- The username for InfluxDB
	 * @param password			- The password for InfluxDB
	 * @param database			- The name of the database (created if does not exist) on
	 * @param tags				- Map of custom tags with custom values
	 * @param writeConsistency	- The write consistency for InfluxDB.
	 * 								<ul>Valid values : 
	 * 									<li>"ALL" (by default)</li>
	 * 									<li>"ANY"</li>
	 * 									<li>"ONE"</li>
	 * 									<li>"QUORUM"</li>
	 * 								</ul>
	 * @param retentionPolicy	- The retention policy for InfluxDB
	 * @param resultTags		- A list of meta-data from the result to add as tags. Sends all meta-data by default
	 * 								<ul>Available data : 
	 * 									<li>"typeName"</li>
	 * 									<li>"objDomain"</li>
	 * 									<li>"className"</li>
	 * 									<li>"attributeName"</li>
	 * 								</ul>
	 * @param createDatabase	- Creates the database in InfluxDB if not found
	 * @param reportJmxPortAsTag - Sends the JMX server port as tag instead of field
	 * @param typeNamesAsTags	- Sends the given list of typeNames as tags instead of fields keys
	 * @param allowStringValues - Allows the OutputWriter to send String Values
	 */
	@JsonCreator
	public InfluxDbWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("url") String url,
			@JsonProperty("username") String username,
			@JsonProperty("password") String password,
			@JsonProperty("database") String database,
			@JsonProperty("tags") ImmutableMap<String, String> tags,
			@JsonProperty("writeConsistency") String writeConsistency,
			@JsonProperty("retentionPolicy") String retentionPolicy,
			@JsonProperty("resultTags") List<String> resultTags,
			@JsonProperty("createDatabase") Boolean createDatabase,
			@JsonProperty("reportJmxPortAsTag") Boolean reportJmxPortAsTag,
			@JsonProperty("typeNamesAsTags") Boolean typeNamesAsTags,
			@JsonProperty("allowStringValues") Boolean allowStringValues) {
		
		this.typeNames = firstNonNull(typeNames,ImmutableList.<String>of());
		this.booleanAsNumber = booleanAsNumber;
		this.database = database;
		this.createDatabase = firstNonNull(createDatabase, TRUE);
		this.typeNamesAsTags = firstNonNull(typeNamesAsTags, FALSE);
		this.allowStringValues = firstNonNull(allowStringValues, FALSE);
		this.writeConsistency = StringUtils.isNotBlank(writeConsistency)
				? InfluxDB.ConsistencyLevel.valueOf(writeConsistency) : InfluxDB.ConsistencyLevel.ALL;
		this.retentionPolicy = StringUtils.isNotBlank(retentionPolicy) ? retentionPolicy : DEFAULT_RETENTION_POLICY;
		this.resultAttributesToWriteAsTags = initResultAttributesToWriteAsTags(resultTags);
		this.tags = initCustomTagsMap(tags);
		
		LOG.debug("Connecting to url: {} as: username: {}", url, username);

		influxDB = InfluxDBFactory.connect(url, username, password);

		this.reportJmxPortAsTag = firstNonNull(reportJmxPortAsTag, FALSE);
	}


	private ImmutableMap<String, String> initCustomTagsMap(ImmutableMap<String, String> tags) {
		return ImmutableMap.copyOf(firstNonNull(tags, Collections.<String,String>emptyMap()));
	}

	private ImmutableSet<ResultAttribute> initResultAttributesToWriteAsTags(List<String> resultTags) {
		ImmutableSet<ResultAttribute> result;
		if (resultTags == null) {
			result = ImmutableSet.copyOf(ResultAttributes.values());
		} else {
			result = ResultAttributes.forNames(resultTags);
		}
		LOG.debug("Result Tags to write set to: {}", result);
		return result;
	}

	@Override
	public ResultTransformerOutputWriter<InfluxDbWriter> create() {
		return ResultTransformerOutputWriter.booleanToNumber(booleanAsNumber, new InfluxDbWriter(influxDB, database,
				writeConsistency, retentionPolicy, tags, resultAttributesToWriteAsTags, typeNames, createDatabase, reportJmxPortAsTag, typeNamesAsTags, allowStringValues));
	}
}
