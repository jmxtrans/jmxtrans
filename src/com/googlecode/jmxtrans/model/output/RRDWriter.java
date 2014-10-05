package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
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

	private File outputFile = null;
	private File templateFile = null;

	@JsonCreator
	public RRDWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, debugEnabled, settings);
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
		outputFile = new File((String) this.getSettings().get(OUTPUT_FILE));
		templateFile = new File((String) this.getSettings().get(TEMPLATE_FILE));

		if (!outputFile.exists() || !templateFile.exists()) {
			throw new ValidationException("output file and template file can't be null", query);
		}
	}

	/** */
	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
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
}
