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
package com.googlecode.jmxtrans.guice;


import com.google.common.io.Closer;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.connections.DatagramSocketFactory;
import com.googlecode.jmxtrans.connections.JMXConnectionParams;
import com.googlecode.jmxtrans.connections.JmxConnectionFactory;
import com.googlecode.jmxtrans.connections.SocketFactory;
import com.googlecode.jmxtrans.monitoring.ManagedGenericKeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.remote.JMXConnector;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class JmxTransModule extends AbstractModule {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JmxTransConfiguration configuration;

	public JmxTransModule(@Nonnull JmxTransConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	protected void configure() {
		bind(new TypeLiteral<GenericKeyedObjectPool<InetSocketAddress, Socket>>(){})
				.toInstance(getObjectPool(new SocketFactory(), SocketFactory.class.getSimpleName()));
		bind(new TypeLiteral<GenericKeyedObjectPool<JMXConnectionParams, JMXConnector>>(){})
				.toInstance(getObjectPool(new JmxConnectionFactory(), JmxConnectionFactory.class.getSimpleName()));
		bind(new TypeLiteral<GenericKeyedObjectPool<SocketAddress, DatagramSocket>>(){})
				.toInstance(getObjectPool(new DatagramSocketFactory(), DatagramSocketFactory.class.getSimpleName()));
	}

	@Provides
	JmxTransConfiguration jmxTransConfiguration() {
		return configuration;
	}

	@Provides
	@Inject
	Scheduler scheduler(JmxTransConfiguration configuration, GuiceJobFactory jobFactory) throws SchedulerException, IOException {
		StdSchedulerFactory serverSchedFact = new StdSchedulerFactory();
		Closer closer = Closer.create();
		try {
			InputStream stream;
			if (configuration.getQuartPropertiesFile() == null) {
				stream = closer.register(JmxTransModule.class.getResourceAsStream("/quartz.server.properties"));
			} else {
				stream = closer.register(new FileInputStream(configuration.getQuartPropertiesFile()));
			}
			serverSchedFact.initialize(stream);
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
		Scheduler scheduler = serverSchedFact.getScheduler();
		scheduler.setJobFactory(jobFactory);
		return scheduler;
	}

	private <K, V> GenericKeyedObjectPool getObjectPool(KeyedPoolableObjectFactory<K, V> factory, String poolName) {
		GenericKeyedObjectPool<K, V> pool = new GenericKeyedObjectPool<K, V>(factory);
		pool.setTestOnBorrow(true);
		pool.setMaxActive(-1);
		pool.setMaxIdle(-1);
		pool.setTimeBetweenEvictionRunsMillis(1000 * 60 * 5);
		pool.setMinEvictableIdleTimeMillis(1000 * 60 * 5);

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


}
