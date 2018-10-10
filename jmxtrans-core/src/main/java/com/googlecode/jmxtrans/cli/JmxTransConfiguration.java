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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;

@SuppressWarnings("squid:S1213") // having instance variables close to their getters is more readable in this class
public class JmxTransConfiguration {
	private static final String CONTINUE_ON_ERROR_PROPERTY = "continue.on.error";
	@Parameter(
			names = {"-c", "--continue-on-error"},
			description = "If it is false, then JmxTrans will stop when one of the JSON configuration file is invalid. " +
					"Otherwise, it will just print an error and continue processing.",
			arity = 1
	)
	@Getter @Setter
	private boolean continueOnJsonError = false;

	private static final String JSON_DIRECTORY_PROPERTY = "json.directory";
	@Parameter(names = {"-j", "--json-directory"}, validateValueWith = ExistingDirectoryValidator.class)
	@Setter private File processConfigDir;

	private static final String JSON_FILE_PROPERTY = "json.file";
	@Parameter(names = {"-f", "--json-file"}, validateValueWith = ExistingFileValidator.class)
	@Setter private File processConfigFile;

	private static final String CONFIG_FILE_PROPERTY = "config.file";
	@Parameter(
			names = {"--config"},
			description = "global jmxtrans configuration file",
			validateValueWith = ExistingFileValidator.class)
	@Getter @Setter private File configFile;

	public File getProcessConfigDirOrFile() {
		if (processConfigDir != null) return processConfigDir;
		return processConfigFile;
	}

	private static final String RUN_ENDLESSLY_PROPERTY = "run.endlessly";
	@Parameter(
			names = {"-e", "--run-endlessly"},
			description = "If this is set, then this class will execute the main() loop and then wait 60 seconds until running again."
	)
	@Getter @Setter
	private boolean runEndlessly = false;

	private static final String QUARTZ_PROPERTIES_FILE_PROPERTY = "quartz.properties.file";
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

	private static final String RUN_PERIOD_IN_SECONDS_PROPERTY = "run.period.in.seconds";
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

	private static final String ADDITIONAL_JARS_PROPERTY = "additional.jars";
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

	private static final String QUERY_PROCESSOR_EXECUTOR_POOL_SIZE_PROPERTY = "query.processor.executor.pool.size";
	@Parameter(
			names = {"--query-processor-executor-pool-size"},
			description = "Number of threads used to process queries.",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int queryProcessorExecutorPoolSize = 10;

	private static final String QUERY_PROCESSOR_EXECUTOR_WORK_QUEUE_CAPACITY_PROPERTY = "query.processor.executor.work.queue.capacity";
	@Parameter(
			names = {"--query-processor-executor-work-queue-capacity"},
			description = "Size of the query work queue",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int queryProcessorExecutorWorkQueueCapacity = 100000;

	private static final String RESULT_PROCESSOR_EXECUTOR_POOL_SIZE_PROPERTY = "result.processor.executor.pool.size";
	@Parameter(
			names = {"--result-processor-executor-pool-size"},
			description = "Number of threads used to process results",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int resultProcessorExecutorPoolSize = 10;

	private static final String RESULT_PROCESSOR_EXECUTOR_WORK_QUEUE_CAPACITY_PROPERTY = "result.processor.executor.work.queue.capacity";
	@Parameter(
			names = {"--result-processor-executor-work-queue-capacity"},
			description = "Size of the result work queue",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int resultProcessorExecutorWorkQueueCapacity = 100000;

	private static final String USE_SEPARATE_EXECUTORS_PROPERTY = "use.separate.executors";
	@Parameter(
			names = {"--use-separate-executors"},
			description = "If this set every server node will be handed by separate executor."
	)
	@Getter @Setter
	private boolean useSeparateExecutors = false;

	private static abstract class PropertySetter<T> {
		private final String key;
		private final Class<T> type;
		public PropertySetter(String key, Class<T> type) {
			this.key = key;
			this.type = type;
		}
		public void setValue(TypedProperties typedProperties, JmxTransConfiguration configuration) {
			T value = typedProperties.getTypedProperty(key, type);
			if (value != null) {
				doSetValue(value, configuration);
			}
		}

		protected abstract void doSetValue(T value, JmxTransConfiguration configuration);
	}

	private static PropertySetter[] SETTERS = new PropertySetter[]{
			new PropertySetter<Boolean>(CONTINUE_ON_ERROR_PROPERTY, Boolean.class) {
				@Override
				protected void doSetValue(Boolean value, JmxTransConfiguration configuration) {
					configuration.setContinueOnJsonError(value);
				}
			},
			new PropertySetter<File>(JSON_DIRECTORY_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setProcessConfigDir(value);
				}
			},
			new PropertySetter<File>(JSON_FILE_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setProcessConfigFile(value);
				}
			},
			new PropertySetter<File>(CONFIG_FILE_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setConfigFile(value);
				}
			},
			new PropertySetter<Boolean>(RUN_ENDLESSLY_PROPERTY, Boolean.class) {
				@Override
				protected void doSetValue(Boolean value, JmxTransConfiguration configuration) {
					configuration.setRunEndlessly(value);
				}
			},
			new PropertySetter<File>(QUARTZ_PROPERTIES_FILE_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setQuartzPropertiesFile(value);
				}
			},
			new PropertySetter<Integer>(RUN_ENDLESSLY_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setRunPeriod(value);
				}
			},
			new PropertySetter<String>(ADDITIONAL_JARS_PROPERTY, String.class) {
				@Override
				protected void doSetValue(String value, JmxTransConfiguration configuration) {
				}
			},
			new PropertySetter<Integer>(QUERY_PROCESSOR_EXECUTOR_POOL_SIZE_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setQueryProcessorExecutorPoolSize(value);
				}
			},
			new PropertySetter<Integer>(QUERY_PROCESSOR_EXECUTOR_WORK_QUEUE_CAPACITY_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setQueryProcessorExecutorWorkQueueCapacity(value);
				}
			},
			new PropertySetter<Integer>(RESULT_PROCESSOR_EXECUTOR_POOL_SIZE_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setResultProcessorExecutorPoolSize(value);
				}
			},
			new PropertySetter<Integer>(RESULT_PROCESSOR_EXECUTOR_WORK_QUEUE_CAPACITY_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setResultProcessorExecutorWorkQueueCapacity(value);
				}
			},
			new PropertySetter<Boolean>(USE_SEPARATE_EXECUTORS_PROPERTY, Boolean.class) {
				@Override
				protected void doSetValue(Boolean value, JmxTransConfiguration configuration) {
					configuration.setUseSeparateExecutors(value);
				}
			}
	};


	@VisibleForTesting
	void loadProperties(Properties properties) {
		TypedProperties typedProperties = new TypedProperties(properties);
		for(PropertySetter setter: SETTERS) {
			setter.setValue(typedProperties, this);
		}
	}

	public void loadProperties(File configFile) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("jmxtrans.properties")) {
			properties.load(in);
		}
		if (configFile != null) {
			try (InputStream in = new FileInputStream(configFile)) {
				properties.load(in);
			}
		} else {
			File defaultSystemProperties = new File("/etc/jmxtrans/jmxtrans.properties");
			if (defaultSystemProperties.isFile()) {
				try (InputStream in = new FileInputStream(defaultSystemProperties)) {
					properties.load(in);
				}
			}
		}
		loadProperties(properties);
	}

}
