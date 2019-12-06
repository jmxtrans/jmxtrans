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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerSchedulerTest {

	@Mock
	private JmxTransConfiguration configuration;
	@Mock
	private ExecutorRepository queryExecutorRepository;
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
		Server server = mock(Server.class);
		when(server.getRunPeriodSeconds()).thenReturn(2);
		when(server.getQueries()).thenReturn(ImmutableSet.<Query>of());
		when(queryExecutorRepository.getExecutor(same(server))).thenReturn(null);
		// When
		serverScheduler.schedule(server);
		// Then
		verify(queryExecutorRepository, timeout(6000).atLeastOnce()).getExecutor(same(server));
	}

	@Test
	public void testScheduleWhenRunFails() throws InterruptedException {
		// Given
		Server server = mock(Server.class);
		when(server.getRunPeriodSeconds()).thenReturn(2);
		when(queryExecutorRepository.getExecutor(same(server))).thenThrow(new IllegalStateException("Command failed"));
		// When
		serverScheduler.schedule(server);
		// Then
		verify(queryExecutorRepository, timeout(6000L).atLeast(2)).getExecutor(same(server));
	}

	@Test
	public void testScheduleWhenRunBlocks() throws InterruptedException {
		// Given
		when(configuration.getRunPeriod()).thenReturn(2);
		//   Server 1
		Server server1 = mock(Server.class);
		when(server1.getHost()).thenReturn("test1");
		when(server1.getQueries()).then(new Answer<ImmutableSet>() {
			@Override
			public ImmutableSet answer(InvocationOnMock invocationOnMock) throws Throwable {
				Thread.sleep(10000L);
				return ImmutableSet.<Query>of();
			}
		});
		when(queryExecutorRepository.getExecutor(same(server1))).thenReturn(null);
		//   Server 2
		Server server2 = mock(Server.class);
		when(server2.getHost()).thenReturn("test2");
		when(server2.getQueries()).thenReturn(ImmutableSet.<Query>of());
		when(queryExecutorRepository.getExecutor(same(server2))).thenReturn(null);
		// When
		serverScheduler.schedule(server1);
		serverScheduler.schedule(server2);
		// Then
		verify(queryExecutorRepository, timeout(6000L).atLeastOnce()).getExecutor(same(server1));
		verify(queryExecutorRepository, timeout(6000L).atLeast(2)).getExecutor(same(server2));
	}
}
