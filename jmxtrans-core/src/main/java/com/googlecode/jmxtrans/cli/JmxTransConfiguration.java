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
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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
		return additionalJars.stream().map(File::new).collect(Collectors.toList());
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

	private static final String SCHEDULED_EXECUTOR_POOL_SIZE_PROPERTY = "scheduled.executor.pool.size";
	@Parameter(
			names = {"--scheduled-executor-pool-size"},
			description = "Number of threads used to run scheduler",
			validateWith = PositiveInteger.class
	)
	@Getter @Setter
	private int scheduledExecutorPoolSize = 2;

	private static abstract class PropertySetter<T> {
		protected final String key;
		protected final Class<T> type;
		protected PropertySetter(String key, Class<T> type) {
			this.key = key;
			this.type = type;
		}
		public abstract void setValue(TypedProperties typedProperties, JmxTransConfiguration configuration);
	}

	private static abstract class SinglePropertySetter<T> extends PropertySetter<T>{
		SinglePropertySetter(String key, Class<T> type) {
			super(key, type);
		}
		public void setValue(TypedProperties typedProperties, JmxTransConfiguration configuration) {
			T value = typedProperties.getTypedProperty(key, type);
			if (value != null) {
				doSetValue(value, configuration);
			}
		}

		protected abstract void doSetValue(T value, JmxTransConfiguration configuration);
	}

	private static abstract class MultiPropertySetter<T> extends PropertySetter<T> {
		MultiPropertySetter(String key, Class<T> type) {
			super(key, type);
		}

		public void setValue(TypedProperties typedProperties, JmxTransConfiguration configuration) {
			List<T> values = typedProperties.getTypedProperties(key, type);
			if (values != null && !values.isEmpty()) {
				doSetValue(values, configuration);
			}
		}

		protected abstract void doSetValue(List<T> value, JmxTransConfiguration configuration);
	}

	private static PropertySetter[] SETTERS = new PropertySetter[]{
			new SinglePropertySetter<Boolean>(CONTINUE_ON_ERROR_PROPERTY, Boolean.class) {
				@Override
				protected void doSetValue(Boolean value, JmxTransConfiguration configuration) {
					configuration.setContinueOnJsonError(value);
				}
			},
			new SinglePropertySetter<File>(JSON_DIRECTORY_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setProcessConfigDir(value);
				}
			},
			new SinglePropertySetter<File>(JSON_FILE_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setProcessConfigFile(value);
				}
			},
			new SinglePropertySetter<File>(CONFIG_FILE_PROPERTY, File.class) {
				@Override
				protected void doSetValue(File value, JmxTransConfiguration configuration) {
					configuration.setConfigFile(value);
				}
			},
			new SinglePropertySetter<Boolean>(RUN_ENDLESSLY_PROPERTY, Boolean.class) {
				@Override
				protected void doSetValue(Boolean value, JmxTransConfiguration configuration) {
					configuration.setRunEndlessly(value);
				}
			},
			new SinglePropertySetter<Integer>(RUN_PERIOD_IN_SECONDS_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setRunPeriod(value);
				}
			},
			new MultiPropertySetter<String>(ADDITIONAL_JARS_PROPERTY, String.class) {
				@Override
				protected void doSetValue(List<String> value, JmxTransConfiguration configuration) {
					configuration.setAdditionalJars(value);
				}
			},
			new SinglePropertySetter<Integer>(QUERY_PROCESSOR_EXECUTOR_POOL_SIZE_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setQueryProcessorExecutorPoolSize(value);
				}
			},
			new SinglePropertySetter<Integer>(QUERY_PROCESSOR_EXECUTOR_WORK_QUEUE_CAPACITY_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setQueryProcessorExecutorWorkQueueCapacity(value);
				}
			},
			new SinglePropertySetter<Integer>(RESULT_PROCESSOR_EXECUTOR_POOL_SIZE_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setResultProcessorExecutorPoolSize(value);
				}
			},
			new SinglePropertySetter<Integer>(RESULT_PROCESSOR_EXECUTOR_WORK_QUEUE_CAPACITY_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setResultProcessorExecutorWorkQueueCapacity(value);
				}
			},
			new SinglePropertySetter<Boolean>(USE_SEPARATE_EXECUTORS_PROPERTY, Boolean.class) {
				@Override
				protected void doSetValue(Boolean value, JmxTransConfiguration configuration) {
					configuration.setUseSeparateExecutors(value);
				}
			},
			new SinglePropertySetter<Integer>(SCHEDULED_EXECUTOR_POOL_SIZE_PROPERTY, Integer.class) {
				@Override
				protected void doSetValue(Integer value, JmxTransConfiguration configuration) {
					configuration.setScheduledExecutorPoolSize(value);
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

}
