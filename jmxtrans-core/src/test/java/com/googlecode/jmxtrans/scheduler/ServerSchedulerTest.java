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
package com.googlecode.jmxtrans.scheduler;

import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.jmx.ResultProcessor;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.monitoring.ManagedThreadPoolExecutor;
import org.apache.commons.pool.KeyedObjectPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerSchedulerTest {

	@Mock
	private JmxTransConfiguration configuration;
	private DummyExecutorRepository queryExecutorRepository = new DummyExecutorRepository();
	@Mock
	private ResultProcessor resultProcessor;

	private ServerScheduler serverScheduler;

	@Before
	public void setUp() {
		serverScheduler = new ServerScheduler(configuration, Executors.newScheduledThreadPool(2), queryExecutorRepository, resultProcessor);
		serverScheduler.start();
	}

	@After
	public void tearDown() {
		serverScheduler.stop();
	}

	@Test
	public void testSchedule() throws InterruptedException {
		// Given
		Server server = Server.builder()
				.setRunPeriodSeconds(2)
				.setPid("1")
				.setPool(mock(KeyedObjectPool.class))
				.addQueries(sampleQueries())
				.build();
		ThreadPoolExecutor executor = queryExecutorRepository.initExecutor(server);
		// When
		serverScheduler.schedule(server);
		// Then
		verify(executor, timeout(6000L).atLeast(2)).submit(any(Runnable.class));
	}

	@Test
	public void testScheduleWhenRunFails() throws InterruptedException {
		// Given
		Server server = Server.builder()
				.setRunPeriodSeconds(2)
				.setPid("2")
				.setPool(mock(KeyedObjectPool.class))
				.addQueries(sampleQueries())
				.build();
		ThreadPoolExecutor executor = queryExecutorRepository.initExecutor(server);
		when(executor.submit(any(Runnable.class))).thenThrow(new IllegalStateException("Command failed"));
		// When
		serverScheduler.schedule(server);
		// Then
		verify(executor, timeout(6000L).atLeast(2)).submit(any(Runnable.class));
	}

	private ImmutableSet<Query> sampleQueries() {
		return ImmutableSet.<Query>of(Query.builder().setObj("namespace:type=T,name=N").build());
	}

	@Test
	public void testScheduleWhenRunBlocks() throws InterruptedException {
		// Given
		when(configuration.getRunPeriod()).thenReturn(1);
		//   Server 1
		Server server1 = Server.builder()
				.setRunPeriodSeconds(2)
				.setHost("test1").setPort("9999")
				.setPool(mock(KeyedObjectPool.class))
				.addQueries(sampleQueries())
				.build();
		ThreadPoolExecutor executor1 = queryExecutorRepository.initExecutor(server1);
		when(executor1.submit(any(Runnable.class))).then(new Answer<Future>() {
			@Override
			public Future answer(InvocationOnMock invocationOnMock) throws Throwable {
				Thread.sleep(10000L);
				return null;
			}
		});
		//   Server 2
		Server server2 = Server.builder()
				.setHost("test2").setPort("9999")
				.setPool(mock(KeyedObjectPool.class))
				.addQueries(sampleQueries())
				.build();
		final ThreadPoolExecutor executor2 = queryExecutorRepository.initExecutor(server2);
		// When
		serverScheduler.schedule(server1);
		serverScheduler.schedule(server2);
		// Then
		verify(executor1, timeout(6000L).atLeast(1)).submit(any(Runnable.class));
		verify(executor2, timeout(6000L).atLeast(2)).submit(any(Runnable.class));
	}

	static class DummyExecutorRepository implements ExecutorRepository {
		Map<String, ThreadPoolExecutor> executorMap = new HashMap<>();

		@Override
		public void remove(Server server) {
			executorMap.remove(getServerId(server));
		}

		@Override
		public void put(Server server) {
			initExecutor(server);
		}

		@Override
		public Collection<ThreadPoolExecutor> getExecutors() {
			return executorMap.values();
		}

		@Override
		public ThreadPoolExecutor getExecutor(Server server) {
			return initExecutor(server);
		}

		@Override
		public Collection<ManagedThreadPoolExecutor> getMBeans() {
			return null;
		}

		public ThreadPoolExecutor initExecutor(Server server) {
			String serverId = getServerId(server);
			ThreadPoolExecutor executor = executorMap.get(serverId);
			if (executor == null) {
				executor = mock(ThreadPoolExecutor.class);
				executorMap.put(serverId, executor);
			}
			return executor;
		}
		private String getServerId(Server server) {
			return server.getPid() == null ? server.getHost() : server.getPid();
		}
	}
}
