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
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.jmxtrans.model.PropertyResolver.resolveList;


/**
 * Writes out data in Nagios format to a file, it should be used with Nagios external command file.
 *
 * @author Denis "Thuck" Doria <denisdoria@gmail.com>
 */
public class NagiosWriter extends BaseOutputWriter {

	protected static final String LOG_PATTERN = "%m%n";
	protected static final int LOG_IO_BUFFER_SIZE_BYTES = 1024;

	private static final String NAGIOS_HOST = "nagiosHost";
	private static final String PREFIX = "prefix";
	private static final String POSFIX = "posfix";
	private static final String FILTERS = "filters";
	private static final String THRESHOLDS = "thresholds";

	protected final Map<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();
	private final ImmutableList<String> filters;
	private final ImmutableList<String> thresholds;
	private final String nagiosHost;
	private final File outputFile;
	private final String prefix;
	private final String suffix;

	protected Logger logger;

	@JsonCreator
	public NagiosWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("filters") List<String> filters,
			@JsonProperty("thresholds") List<String> thresholds,
			@JsonProperty("nagiosHost") String nagiosHost,
			@JsonProperty("outputFile") String outputFile,
			@JsonProperty("prefix") String prefix,
			@JsonProperty("suffix") String suffix,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		this.filters = resolveList(firstNonNull(
				filters,
				(List<String>) getSettings().get(FILTERS),
				ImmutableList.<String>of()));
		this.thresholds = resolveList(firstNonNull(
				thresholds,
				(List<String>) getSettings().get(THRESHOLDS),
				ImmutableList.<String>of()));
		this.nagiosHost = MoreObjects.firstNonNull(nagiosHost, (String) getSettings().get(NAGIOS_HOST));
		this.outputFile = new File(MoreObjects.firstNonNull(outputFile, (String) this.getSettings().get("outputFile")));


		this.prefix = firstNonNull(prefix, (String) this.getSettings().get(PREFIX), "");
		this.suffix = firstNonNull(suffix, (String) this.getSettings().get(POSFIX), "");

		if (filters.size() != thresholds.size()) {
			throw new IllegalStateException("filters and thresholds must have the same size.");
		}
	}

	/**
	 * Initial log setup.
	 */
	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
		checkFile(query);
	}

	/**
	 * Creates the logging. Nagios doesn't start if the external command pipe
	 * exists, so we write to /dev/null until we have it available.
	 * From the official documentation:
	 * <blockquote>
	 * The external command file is implemented as a named pipe (FIFO),
	 * which is created when Nagios starts and removed when it shuts down.
	 * If the file exists when Nagios starts,
	 * the Nagios process will terminate with an error message.
	 * http://nagios.sourceforge.net/docs/3_0/configmain.html#command_file
	 * </blockquote>
	 */
	public void checkFile(Query query) throws ValidationException {
		if (!outputFile.exists()) {
			if (loggers.containsKey("/dev/null")) {
				logger = loggers.get("/dev/null");
			} else {
				try {
					logger = initLogger("/dev/null");
					loggers.put("/dev/null", logger);
				} catch (IOException e) {
					throw new ValidationException("Failed to setup log4j", query);
				}
			}

			if (loggers.containsKey(outputFile.getAbsolutePath())) {
				loggers.remove(outputFile.getAbsolutePath());
			}
			return;
		} else if (loggers.containsKey(outputFile.getAbsolutePath())) {
			logger = loggers.get(outputFile.getAbsolutePath());
			return;
		}

		try {
			logger = initLogger(outputFile.getAbsolutePath());
			loggers.put(outputFile.getAbsolutePath(), logger);
		} catch (IOException e) {
			throw new ValidationException("Failed to setup log4j", query);

		}
	}


	/**
	 * The meat of the output. Nagios format..
	 */
	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		checkFile(query);
		List<String> typeNames = getTypeNames();

		for (Result result : results) {
			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Entry<String, Object> values : resultValues.entrySet()) {
					String[] str_array = KeyUtils.getKeyString(server, query, result, values, typeNames, null).split("\\.");
					if (NumberUtils.isNumeric(values.getValue()) && filters.contains(str_array[2])) {
						int threshold_pos = filters.indexOf(str_array[2]);
						StringBuilder sb = new StringBuilder();

						sb.append("[");
						sb.append(result.getEpoch());
						sb.append("] PROCESS_SERVICE_CHECK_RESULT;");
						sb.append(nagiosHost);
						sb.append(";");
						if (prefix != null) {
							sb.append(prefix);
						}
						sb.append(str_array[2]);
						if (suffix != null) {
							sb.append(suffix);
						}
						sb.append(";");
						sb.append(nagiosCheckValue(values.getValue().toString(), thresholds.get(threshold_pos)));
						sb.append(";");
						//Missing the performance information

						logger.info(sb.toString());
					}
				}
			}
		}
	}

	/**
	 * Initializes the logger. This is called when we need to create a new
	 * logger for the given file name.
	 *
	 * @param fileStr
	 * @return a new Logger instance for the given fileStr
	 * @throws IOException
	 */
	protected Logger initLogger(String fileStr) throws IOException {
		PatternLayout pl = new PatternLayout(LOG_PATTERN);

		final FileAppender appender = new FileAppender(pl, fileStr, true);
		appender.setBufferedIO(false);
		appender.setBufferSize(LOG_IO_BUFFER_SIZE_BYTES);

		LoggerFactory loggerFactory = new LoggerFactory() {
			@Override
			public Logger makeNewLoggerInstance(String name) {
				Logger logger = Logger.getLogger(name);
				logger.addAppender(appender);
				logger.setLevel(Level.INFO);
				logger.setAdditivity(false);
				return logger;
			}
		};
		return loggerFactory.makeNewLoggerInstance("NagiosWriter" + this.hashCode());
	}

	/**
	 * Define if a value is in a critical, warning or ok state.
	 */
	protected String nagiosCheckValue(String value, String composeRange) {
		List<String> simpleRange = Arrays.asList(composeRange.split(","));
		double value_d = Double.parseDouble(value);

		if (composeRange.isEmpty()) {
			return "0";
		}

		if (simpleRange.size() == 1) {
			if (composeRange.endsWith(",")) {
				if (valueCheck(value_d, simpleRange.get(0))) {
					return "1";
				} else {
					return "0";
				}
			} else if (valueCheck(value_d, simpleRange.get(0))) {
				return "2";

			} else {
				return "0";
			}
		}

		if (valueCheck(value_d, simpleRange.get(1))) {
			return "2";
		}

		if (valueCheck(value_d, simpleRange.get(0))) {
			return "1";

		}
		return "0";
	}

	/**
	 * Check if a value is inside of a range defined in the thresholds.
	 * This check is based on Nagios range definition.
	 * http://nagiosplug.sourceforge.net/developer-guidelines.html#THRESHOLDFORMAT
	 */
	protected boolean valueCheck(double value, String simpleRange) {
		if (simpleRange.isEmpty()) {
			return false;
		}

		if (simpleRange.endsWith(":")) {
			return value < Double.parseDouble(simpleRange.replace(":", ""));

		}

		if (simpleRange.startsWith("~:")) {
			return value > Double.parseDouble(simpleRange.replace("~:", ""));

		}

		if (simpleRange.startsWith("@")) {
			String[] values = simpleRange.replace("@", "").split(":");
			return value >= Double.parseDouble(values[0]) && value <= Double.parseDouble(values[1]);
		}

		if (simpleRange.matches("^-{0,1}[0-9]+:-{0,1}[0-9]+$")) {
			String[] values = simpleRange.split(":");
			return value < Double.parseDouble(values[0]) || value > Double.parseDouble(values[1]);
		}

		return simpleRange.matches("^-{0,1}[0-9]+$") && (0 > value || value > Double.parseDouble(simpleRange));
	}

	public ImmutableList<String> getFilters() {
		return filters;
	}

	public ImmutableList<String> getThresholds() {
		return thresholds;
	}

	public String getNagiosHost() {
		return nagiosHost;
	}

	public String getOutputFile() {
		return outputFile.getPath();
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}
}
