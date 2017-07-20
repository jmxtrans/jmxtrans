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
package com.googlecode.jmxtrans.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

@SuppressWarnings("squid:S1213") // having instance variables close to their getters is more readable in this class
public class JmxTransConfiguration {
	@Parameter(
			names = {"-c", "--continue-on-error"},
			description = "If it is false, then JmxTrans will stop when one of the JSON configuration file is invalid. " +
					"Otherwise, it will just print an error and continue processing.",
			arity = 1
	)
	@Getter @Setter
	private boolean continueOnJsonError = false;

	@Parameter(names = {"-j", "--json-directory"}, validateValueWith = ExistingDirectoryValidator.class)
	@Setter private File processConfigDir;
	@Parameter(names = {"-f", "--json-file"}, validateValueWith = ExistingFileValidator.class)
	@Setter private File processConfigFile;

	@Parameter(
			names = {"--config"},
			description = "global jmxtrans configuration file",
			validateValueWith = ExistingFileValidator.class)
	@Getter @Setter private File configFile;

	public File getProcessConfigDirOrFile() {
		if (processConfigDir != null) return processConfigDir;
		return processConfigFile;
	}

	@Parameter(
			names = {"-e", "--run-endlessly"},
			description = "If this is set, then this class will execute the main() loop and then wait 60 seconds until running again."
	)
	@Getter @Setter
	private boolean runEndlessly = false;
	/**
	 * The Quartz server properties.
	 */
	@Parameter(
			names = {"-q", "--quartz-properties-file"},
			description = "The Quartz server properties.",
			validateValueWith = ExistingFileValidator.class
	)
	@Getter @Setter
	private File quartzPropertiesFile = null;

	/**
	 * The seconds between server job runs.
	 */
	@Parameter(
			names = {"-s", "--run-period-in-seconds"},
			description = "The seconds between server job runs.",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int runPeriod = 60;

	@Parameter(names = {"-h", "--help"}, help = true)
	@Getter @Setter
	private boolean help = false;

	@Parameter(
			names = {"-a", "--additional-jars"},
			validateWith = ExistingFilenameValidator.class,
			variableArity = true
	)
	@Setter
	private List<String> additionalJars = ImmutableList.of();
	public Iterable<File> getAdditionalJars() {
		return FluentIterable.from(additionalJars)
				.transform(new Function<String, File>() {
					@Nullable
					@Override
					public File apply(String input) {
						return new File(input);
					}
				})
				.toList();
	}

	@Parameter(
			names = {"--query-processor-executor-pool-size"},
			description = "Number of threads used to process queries.",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int queryProcessorExecutorPoolSize = 10;

	@Parameter(
			names = {"--query-processor-executor-work-queue-capacity"},
			description = "Size of the query work queue",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int queryProcessorExecutorWorkQueueCapacity = 100000;

	@Parameter(
			names = {"--result-processor-executor-pool-size"},
			description = "Number of threads used to process results",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int resultProcessorExecutorPoolSize = 10;

	@Parameter(
			names = {"--result-processor-executor-work-queue-capacity"},
			description = "Size of the result work queue",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int resultProcessorExecutorWorkQueueCapacity = 100000;

}
