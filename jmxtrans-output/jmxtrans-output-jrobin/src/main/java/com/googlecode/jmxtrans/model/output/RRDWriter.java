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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.commons.io.FileUtils;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdDefTemplate;
import org.jrobin.core.Sample;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkState;

/**
 * This takes a JRobin template.xml file and then creates the database if it
 * doesn't already exist.
 * 
 * It will then write the contents of the Query (the Results) to the database.
 * 
 * This uses the JRobin rrd format and is incompatible with the C version of
 * rrd.
 * 
 * @author jon
 */
public class RRDWriter extends BaseOutputWriter {

	private final File outputFile;
	private final File templateFile;

	@JsonCreator
	public RRDWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("templateFile") String templateFile,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.outputFile = new File(MoreObjects.firstNonNull(outputFile, (String) getSettings().get(OUTPUT_FILE)));
		this.templateFile = new File(MoreObjects.firstNonNull(templateFile, (String) getSettings().get(TEMPLATE_FILE)));
		checkState(this.outputFile.exists(), "Output file must exist");
		checkState(this.templateFile.exists(), "Template file must exist");
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		RrdDb db = null;
		try {
			db = createOrOpenDatabase();
			Sample sample = db.createSample();
			List<String> dsNames = Arrays.asList(db.getDsNames());

			// go over all the results and look for datasource names that map to
			// keys from the result values
			for (Result res : results) {
				Map<String, Object> values = res.getValues();
				if (values != null) {
					for (Entry<String, Object> entry : values.entrySet()) {
						if (dsNames.contains(entry.getKey()) && NumberUtils.isNumeric(entry.getValue())) {
							sample.setValue(entry.getKey(), Double.valueOf(entry.getValue().toString()));
						}
					}
				}
			}
			sample.update();
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	/**
	 * If the database file doesn't exist, it'll get created, otherwise, it'll
	 * be returned in r/w mode.
	 */
	protected RrdDb createOrOpenDatabase() throws Exception {
		RrdDb result;
		if (!this.outputFile.exists()) {
			FileUtils.forceMkdir(this.outputFile.getParentFile());
			RrdDefTemplate t = new RrdDefTemplate(this.templateFile);
			t.setVariable("database", this.outputFile.getCanonicalPath());
			RrdDef def = t.getRrdDef();
			result = new RrdDb(def);
		} else {
			result = new RrdDb(this.outputFile.getCanonicalPath());
		}
		return result;
	}

	public String getTemplateFile() {
		return templateFile.getPath();
	}

	public String getOutputFile() {
		return outputFile.getPath();
	}
}
