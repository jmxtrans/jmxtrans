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
package com.googlecode.jmxtrans.cli;

import com.google.common.collect.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;

public class CliArgumentParser {
	/**
	 * Parse the options given on the command line.
	 *
	 * @param args
	 */
	public JmxTransConfiguration parseOptions(String[] args) throws OptionsException, org.apache.commons.cli.ParseException {
		CommandLineParser parser = new GnuParser();
		CommandLine cl = parser.parse(getOptions(), args);
		Option[] options = cl.getOptions();

		JmxTransConfiguration configuration = new JmxTransConfiguration();

		for (Option option : options) {
			if (option.getOpt().equals("c")) {
				configuration.setContinueOnJsonError(Boolean.parseBoolean(option.getValue()));
			} else if (option.getOpt().equals("j")) {
				File jsonDir = new File(option.getValue());
				if (jsonDir.exists() && jsonDir.isDirectory()) {
					configuration.setJsonDirOrFile(jsonDir);
				} else {
					throw new OptionsException("Path to json directory is invalid: " + jsonDir);
				}
			} else if (option.getOpt().equals("f")) {
				File jsonFile = new File(option.getValue());
				if (jsonFile.exists() && jsonFile.isFile()) {
					configuration.setJsonDirOrFile(jsonFile);
				} else {
					throw new OptionsException("Path to json file is invalid: " + jsonFile);
				}
			} else if (option.getOpt().equals("e")) {
				configuration.setRunEndlessly(true);
			} else if (option.getOpt().equals("q")) {
				File quartzConfigFile = new File(option.getValue());
				if (quartzConfigFile.exists() && quartzConfigFile.isFile()) {
					configuration.setQuartPropertiesFile(option.getValue());
				} else {
					throw new OptionsException("Could not find path to the quartz properties file: "
							+ quartzConfigFile.getAbsolutePath());
				}
			} else if (option.getOpt().equals("s")) {
				try {
					configuration.setRunPeriod(Integer.parseInt(option.getValue()));
				} catch (NumberFormatException nfe) {
					throw new OptionsException("Seconds between server job runs must be an integer");
				}
			} else if (option.getOpt().equals("a")) {
				ImmutableList.Builder<File> jars = ImmutableList.builder();
				for (String jar : option.getValues()) {
					jars.add(new File(jar));
				}
				configuration.setAdditionalJars(jars.build());
			} else if (option.getOpt().equals("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar jmxtrans-all.jar", getOptions());
				configuration.setHelp(true);
			}
		}
		if ((!configuration.isHelp()) && (configuration.getJsonDirOrFile() == null)) {
			throw new OptionsException("Please specify either the -f or -j option.");
		}
		return configuration;
	}

	private Options getOptions() {
		Options options = new Options();
		options.addOption("c", true, "Continue processing even if one of the JSON configuration file is invalid.");
		options.addOption("j", true, "Directory where json configuration is stored. Default is .");
		options.addOption("f", true, "A single json file to execute.");
		options.addOption("e", false, "Run endlessly. Default false.");
		options.addOption("q", true, "Path to quartz configuration file.");
		options.addOption("s", true, "Seconds between server job runs (not defined with cron). Default: 60");
		options.addOption(OptionBuilder
				.withArgName("a")
				.withLongOpt("additionalJars")
				.hasArgs()
				.withValueSeparator(',')
				.withDescription("Comma delimited list of additional jars to add to the class path")
				.create("a"));
		options.addOption("h", false, "Help");
		return options;
	}
}
