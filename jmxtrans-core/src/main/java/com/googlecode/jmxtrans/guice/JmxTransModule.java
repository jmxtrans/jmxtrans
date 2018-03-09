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
package com.googlecode.jmxtrans.guice;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.connections.DatagramSocketFactory;
import com.googlecode.jmxtrans.connections.JMXConnection;
import com.googlecode.jmxtrans.connections.JmxConnectionProvider;
import com.googlecode.jmxtrans.connections.MBeanServerConnectionFactory;
import com.googlecode.jmxtrans.connections.SocketFactory;
import com.googlecode.jmxtrans.executors.CommonExecutorRepository;
import com.googlecode.jmxtrans.executors.ExecutorFactory;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.executors.SeparateExecutorRepository;
import com.googlecode.jmxtrans.monitoring.ManagedGenericKeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.MalformedObjectNameException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class JmxTransModule extends AbstractModule {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JmxTransConfiguration configuration;

	public JmxTransModule(@Nonnull JmxTransConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	protected void configure() {
		bind(new TypeLiteral<GenericKeyedObjectPool<InetSocketAddress, Socket>>(){})
				.toInstance(getObjectPool(new SocketFactory(), SocketFactory.class.getSimpleName(), 0));
		bind(new TypeLiteral<GenericKeyedObjectPool<SocketAddress, DatagramSocket>>(){})
				.toInstance(getObjectPool(new DatagramSocketFactory(), DatagramSocketFactory.class.getSimpleName(), 0));
		bind(new TypeLiteral<KeyedObjectPool<JmxConnectionProvider, JMXConnection>>(){}).annotatedWith(Names.named("mbeanPool"))
				.toInstance(getObjectPool(new MBeanServerConnectionFactory(), MBeanServerConnectionFactory.class.getSimpleName(), 20000));
	}

	@Provides
	JmxTransConfiguration jmxTransConfiguration() {
		return configuration;
	}

	@Provides
	@Inject
	Scheduler scheduler(JmxTransConfiguration configuration, GuiceJobFactory jobFactory) throws SchedulerException, IOException {
		StdSchedulerFactory serverSchedFact = new StdSchedulerFactory();
		try (InputStream stream = openQuartzConfiguration(configuration)) {
			serverSchedFact.initialize(stream);
		}
		Scheduler scheduler = serverSchedFact.getScheduler();
		scheduler.setJobFactory(jobFactory);
		return scheduler;
	}

	private InputStream openQuartzConfiguration(JmxTransConfiguration configuration) throws FileNotFoundException {
		if (configuration.getQuartzPropertiesFile() == null) {
			return JmxTransModule.class.getResourceAsStream("/quartz.server.properties");
		} else {
			return new FileInputStream(configuration.getQuartzPropertiesFile());
		}
	}

	@Provides
	@Singleton
	@Named("queryExecutorRepository")
	ExecutorRepository queryExecutorRepository() throws MalformedObjectNameException {
		int poolSize = configuration.getQueryProcessorExecutorPoolSize();
		int workQueueCapacity = configuration.getQueryProcessorExecutorWorkQueueCapacity();
		String executorAlias = "query";
		return createExecutorRepository(poolSize, workQueueCapacity, executorAlias);
	}

	@Provides
	@Singleton
	@Named("resultExecutorRepository")
	ExecutorRepository resultExecutorRepository() throws MalformedObjectNameException {
		int poolSize = configuration.getResultProcessorExecutorPoolSize();
		int workQueueCapacity = configuration.getResultProcessorExecutorWorkQueueCapacity();
		String executorAlias = "result";
		return createExecutorRepository(poolSize, workQueueCapacity, executorAlias);
	}

	private ExecutorRepository createExecutorRepository(int poolSize, int workQueueCapacity, String executorAlias) throws MalformedObjectNameException {
		final ExecutorFactory executorFactory = new ExecutorFactory(poolSize, workQueueCapacity, executorAlias);
		final boolean useSeparateExecutors = configuration.isUseSeparateExecutors();
		return useSeparateExecutors
			? new SeparateExecutorRepository(executorFactory)
			: new CommonExecutorRepository(executorFactory);
	}

	private <K, V> GenericKeyedObjectPool<K, V> getObjectPool(KeyedPoolableObjectFactory<K, V> factory, String poolName, long maxWaitMillis) {
		GenericKeyedObjectPool<K, V> pool = new GenericKeyedObjectPool<>(factory);
		pool.setTestOnBorrow(true);
		pool.setMaxActive(-1);
		pool.setMaxIdle(-1);
		pool.setTimeBetweenEvictionRunsMillis(MILLISECONDS.convert(5, MINUTES));
		pool.setMinEvictableIdleTimeMillis(MILLISECONDS.convert(5, MINUTES));
		pool.setMaxWait(maxWaitMillis);
		pool.setTestOnReturn(true);
		pool.setTestOnBorrow(true);

		try {
			ManagedGenericKeyedObjectPool mbean =
					new ManagedGenericKeyedObjectPool(
							pool,
							poolName);
			ManagementFactory.getPlatformMBeanServer()
					.registerMBean(mbean, mbean.getObjectName());
		} catch (Exception e) {
			log.error("Could not register mbean for pool [{}]", poolName, e);
		}

		return pool;
	}

	@Nonnull
	public static Injector createInjector(@Nonnull JmxTransConfiguration configuration) {
		return Guice.createInjector(
				new JmxTransModule(configuration),
				new ObjectMapperModule(JsonFormat.class)
						.registerModule(new GuavaModule()),
				new ObjectMapperModule(YamlFormat.class)
						.withObjectMapper(new ObjectMapper(new YAMLFactory()))
						.registerModule(new GuavaModule())
		);
	}

}
