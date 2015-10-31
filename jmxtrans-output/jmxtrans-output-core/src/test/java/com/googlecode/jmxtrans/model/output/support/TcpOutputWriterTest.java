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
package com.googlecode.jmxtrans.model.output.support;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.pool.SocketPoolable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import stormpot.Allocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.LifecycledPool;
import stormpot.Slot;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Callable;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TcpOutputWriterTest {

	private LifecycledPool<SocketPoolable> pool;
	@Spy private DummyAllocator allocator = new DummyAllocator();
	@Mock private WriterBasedOutputWriter target;
	private Writer writer = new StringWriter();
	@Captor private ArgumentCaptor<Writer> writerCaptor;

	@Before
	public void setupPool() {
		Config<SocketPoolable> config = new Config<SocketPoolable>()
				.setAllocator(allocator);
		pool = new BlazePool<SocketPoolable>(config);
	}

	@Test
	public void writerIsPassedToTargetOutputWriter() throws Exception {
		TcpOutputWriter<WriterBasedOutputWriter> outputWriter = new TcpOutputWriter<WriterBasedOutputWriter>(target, pool);

		outputWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());

		verify(target).write(writerCaptor.capture(), any(Server.class), any(Query.class), any(ImmutableList.class));

		assertThat(writerCaptor.getValue()).isSameAs(writer);
	}

	@Test(expected = IOException.class)
	public void socketIsReleasedOnIOException() throws Exception {
		doThrow(IOException.class).when(target).write(any(Writer.class), any(Server.class), any(Query.class), any(ImmutableList.class));

		TcpOutputWriter<WriterBasedOutputWriter> outputWriter = new TcpOutputWriter<WriterBasedOutputWriter>(target, pool);
		try {
			outputWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());
		} finally {
			await()
					.atMost(500, MILLISECONDS)
					.until(socketDeallocated());
		}
	}

	private Callable<Boolean> socketDeallocated() {
		return new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					verify(allocator).deallocate(any(SocketPoolable.class));
				} catch (IOException e) {
					return false;
				}
				return true;
			}
		};
	}


	private class DummyAllocator implements Allocator<SocketPoolable> {
		@Override
		public SocketPoolable allocate(Slot slot) throws Exception {
			return new SocketPoolable(slot, null, writer);
		}

		@Override
		public void deallocate(SocketPoolable poolable) throws Exception {
		}
	}
}
