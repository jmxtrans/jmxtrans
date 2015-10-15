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
import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdDefTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkState;

/**
 * This takes a JRobin template.xml file and then creates the database if it
 * doesn't already exist.
 * 
 * It will then write the contents of the Query (the Results) to the database.
 * 
 * This method exec's out to use the command line version of rrdtool. You need
 * to specify the path to the directory where the binary rrdtool lives.
 * 
 * @author jon
 */
public class RRDToolWriter extends BaseOutputWriter {
	private static final Logger log = LoggerFactory.getLogger(RRDToolWriter.class);

	public static final String GENERATE = "generate";
	private static final char[] INITIALS = { ' ', '.' };

	private final File outputFile;
	private final File templateFile;
	private final File binaryPath;
	private final boolean generate;

	@JsonCreator
	public RRDToolWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("templateFile") String templateFile,
			@JsonProperty("binaryPath") String binaryPath,
			@JsonProperty("generate") Boolean generate,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.outputFile = new File(MoreObjects.firstNonNull(outputFile, (String) getSettings().get(OUTPUT_FILE)));
		this.templateFile = new File(MoreObjects.firstNonNull(templateFile, (String) getSettings().get(TEMPLATE_FILE)));
		this.binaryPath = new File(MoreObjects.firstNonNull(binaryPath, (String) getSettings().get(BINARY_PATH)));
		this.generate = MoreObjects.firstNonNull(generate, Settings.getBooleanSetting(getSettings(), "generate", Boolean.TRUE));

		checkState(this.outputFile.exists(), "Output file must exist");
		checkState(this.templateFile.exists(), "Template file must exist");
		checkState(this.binaryPath.exists(), "RRD Binary must exist");
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	/**
	 * rrd datasources must be less than 21 characters in length, so work to
	 * make it shorter. Not ideal at all, but works fairly well it seems.
	 */
	public String getDataSourceName(String typeName, String attributeName, String entry) {
		String result;
		if (typeName != null) {
			result = typeName + attributeName + entry;
		} else {
			result = attributeName + entry;
		}

		if (attributeName.length() > 15) {
			String[] split = StringUtils.splitByCharacterTypeCamelCase(attributeName);
			String join = StringUtils.join(split, '.');
			attributeName = WordUtils.initials(join, INITIALS);
		}
		result = attributeName + DigestUtils.md5Hex(result);

		result = StringUtils.left(result, 19);

		return result;
	}

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		RrdDef def = getDatabaseTemplateSpec();

		List<String> dsNames = getDsNames(def.getDsDefs());

		Map<String, String> dataMap = new TreeMap<String, String>();

		// go over all the results and look for datasource names that map to
		// keys from the result values
		for (Result res : results) {
			log.debug(res.toString());
			Map<String, Object> values = res.getValues();
			if (values != null) {
				for (Entry<String, Object> entry : values.entrySet()) {
					String key = getDataSourceName(getConcatedTypeNameValues(res.getTypeName()), res.getAttributeName(), entry.getKey());
					boolean isNumeric = NumberUtils.isNumeric(entry.getValue());

					if (isDebugEnabled() && isNumeric) {
						log.debug("Generated DataSource name:value: " + key + " : " + entry.getValue());
					}

					if (dsNames.contains(key) && isNumeric) {
						dataMap.put(key, entry.getValue().toString());
					}
				}
			}
		}

		doGenerate(results);

		if (dataMap.keySet().size() > 0 && dataMap.values().size() > 0) {
			rrdToolUpdate(StringUtils.join(dataMap.keySet(), ':'), StringUtils.join(dataMap.values(), ':'));
		} else {
			log.error("Nothing was logged for query: " + query);
		}
	}

	private void doGenerate(List<Result> results) throws Exception {
		if (isDebugEnabled() && generate) {
			StringBuilder sb = new StringBuilder("\n");
			List<String> keys = new ArrayList<String>();

			for (Result res : results) {
				Map<String, Object> values = res.getValues();
				if (values != null) {
					for (Entry<String, Object> entry : values.entrySet()) {
						boolean isNumeric = NumberUtils.isNumeric(entry.getValue());
						if (isNumeric) {
							String key = getDataSourceName(getConcatedTypeNameValues(res.getTypeName()), res.getAttributeName(), entry.getKey());
							if (keys.contains(key)) {
								throw new Exception("Duplicate datasource name found: '" + key
										+ "'. Please try to add more typeName keys to the writer to make the name more unique. " + res.toString());
							}
							keys.add(key);

							sb.append("<datasource><!-- ").append(res.getTypeName()).append(":")
									.append(res.getAttributeName()).append(":").append(entry.getKey())
									.append(" --><name>").append(key)
									.append("</name><type>GAUGE</type><heartbeat>400</heartbeat><min>U</min><max>U</max></datasource>\n");
						}
					}
				}
			}
			log.debug(sb.toString());
		}
	}

	/**
	 * Executes the rrdtool update command.
	 */
	protected void rrdToolUpdate(String template, String data) throws Exception {
		List<String> commands = new ArrayList<String>();
		commands.add(binaryPath + "/rrdtool");
		commands.add("update");
		commands.add(outputFile.getCanonicalPath());
		commands.add("-t");
		commands.add(template);
		commands.add("N:" + data);

		ProcessBuilder pb = new ProcessBuilder(commands);
		Process process = pb.start();
		checkErrorStream(process);
	}

	/**
	 * If the database file doesn't exist, it'll get created, otherwise, it'll
	 * be returned in r/w mode.
	 */
	protected RrdDef getDatabaseTemplateSpec() throws Exception {
		RrdDefTemplate t = new RrdDefTemplate(templateFile);
		t.setVariable("database", this.outputFile.getCanonicalPath());
		RrdDef def = t.getRrdDef();
		if (!this.outputFile.exists()) {
			FileUtils.forceMkdir(this.outputFile.getParentFile());
			rrdToolCreateDatabase(def);
		}
		return def;
	}

	/**
	 * Calls out to the rrdtool binary with the 'create' command.
	 */
	protected void rrdToolCreateDatabase(RrdDef def) throws Exception {
		List<String> commands = new ArrayList<String>();
		commands.add(this.binaryPath + "/rrdtool");
		commands.add("create");
		commands.add(this.outputFile.getCanonicalPath());
		commands.add("-s");
		commands.add(String.valueOf(def.getStep()));

		for (DsDef dsdef : def.getDsDefs()) {
			commands.add(getDsDefStr(dsdef));
		}

		for (ArcDef adef : def.getArcDefs()) {
			commands.add(getRraStr(adef));
		}

		ProcessBuilder pb = new ProcessBuilder(commands);
		Process process = pb.start();
		try {
			checkErrorStream(process);
		} finally {
			IOUtils.closeQuietly(process.getInputStream());
			IOUtils.closeQuietly(process.getOutputStream());
			IOUtils.closeQuietly(process.getErrorStream());
		}
	}

	/**
	 * Check to see if there was an error processing an rrdtool command
	 */
	private void checkErrorStream(Process process) throws Exception {
		Closer closer = Closer.create();
		try {
			InputStream is = closer.register(process.getErrorStream());
			// rrdtool should use platform encoding (unless you did something
			// very strange with your installation of rrdtool). So let's be
			// explicit and use the presumed correct encoding to read errors.
			InputStreamReader isr = closer.register(new InputStreamReader(is, Charset.defaultCharset()));
			BufferedReader br = closer.register(new BufferedReader(isr));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			if (sb.length() > 0) {
				throw new RuntimeException(sb.toString());
			}
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	/**
	 * Generate a RRA line for rrdtool
	 */
	private String getRraStr(ArcDef def) {
		return "RRA:" + def.getConsolFun() + ":" + def.getXff() + ":" + def.getSteps() + ":" + def.getRows();
	}

	/**
	 * "rrdtool create temperature.rrd --step 300 \\\n" +
	 * "         DS:temp:GAUGE:600:-273:5000 \\\n" +
	 * "         RRA:AVERAGE:0.5:1:1200 \\\n" +
	 * "         RRA:MIN:0.5:12:2400 \\\n" + "         RRA:MAX:0.5:12:2400 \\\n"
	 * + "         RRA:AVERAGE:0.5:12:2400"
	 */
	private String getDsDefStr(DsDef def) {
		return "DS:" + def.getDsName() + ":" + def.getDsType() + ":" + def.getHeartbeat() + ":" + formatDouble(def.getMinValue()) + ":"
				+ formatDouble(def.getMaxValue());
	}

	/**
	 * Get a list of DsNames used to create the datasource.
	 */
	private List<String> getDsNames(DsDef[] defs) {
		List<String> names = new ArrayList<String>();
		for (DsDef def : defs) {
			names.add(def.getDsName());
		}
		return names;
	}

	/**
	 * If dbl is NaN, then return U
	 */
	private String formatDouble(double dbl) {
		if (Double.isNaN(dbl)) {
			return "U";
		}
		return String.valueOf(dbl);
	}

	public String getOutputFile() {
		return outputFile.getPath();
	}

	public String getTemplateFile() {
		return templateFile.getPath();
	}

	public String getBinaryPath() {
		return binaryPath.getPath();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final ImmutableList.Builder<String> typeNames = ImmutableList.builder();
		private boolean booleanAsNumber;
		private Boolean debugEnabled;
		private File outputFile;
		private File templateFile;
		private File binaryPath;
		private Boolean generate;

		private Builder() {}

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

		public Builder setOutputFile(File outputFile) {
			this.outputFile = outputFile;
			return this;
		}

		public Builder setTemplateFile(File templateFile) {
			this.templateFile = templateFile;
			return this;
		}

		public Builder setBinaryPath(File binaryPath) {
			this.binaryPath = binaryPath;
			return this;
		}

		public Builder setGenerate(Boolean generate) {
			this.generate = generate;
			return this;
		}

		public RRDToolWriter build() {
			return new RRDToolWriter(
					typeNames.build(),
					booleanAsNumber,
					debugEnabled,
					outputFile.getPath(),
					templateFile.getPath(),
					binaryPath.getPath(),
					generate,
					null
			);
		}
	}

}
