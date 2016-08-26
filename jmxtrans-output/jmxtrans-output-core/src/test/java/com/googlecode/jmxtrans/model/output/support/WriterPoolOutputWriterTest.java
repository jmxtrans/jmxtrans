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
package com.googlecode.jmxtrans.model.output.support;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.pool.NeverFlush;
import com.googlecode.jmxtrans.model.output.support.pool.SocketPoolable;
import com.googlecode.jmxtrans.model.output.support.pool.WriterPoolable;
import com.googlecode.jmxtrans.test.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import stormpot.Timeout;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WriterPoolOutputWriterTest {

	private LifecycledPool<WriterPoolable> pool;
	@Spy private DummyAllocator allocator = new DummyAllocator();
	@Mock private WriterBasedOutputWriter target;
	private Writer writer = new StringWriter();
	@Captor private ArgumentCaptor<Writer> writerCaptor;

	@Before
	public void setupPool() {
		Config<WriterPoolable> config = new Config<WriterPoolable>()
				.setAllocator(allocator);
		pool = new BlazePool<>(config);
	}

	@Test
	public void writerIsPassedToTargetOutputWriter() throws Exception {
		WriterPoolOutputWriter<WriterBasedOutputWriter> outputWriter = new WriterPoolOutputWriter<>(target, pool, new Timeout(1, SECONDS));

		outputWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());

		verify(target).write(writerCaptor.capture(), any(Server.class), any(Query.class), any(ImmutableList.class));

		assertThat(writerCaptor.getValue()).isSameAs(writer);
	}

	@Test(expected = IllegalStateException.class)
	public void claimTimeout() throws Exception {
		Timeout sleepTime = new Timeout(10, SECONDS);
		Timeout claimTime = new Timeout(1, SECONDS);

		Config<WriterPoolable> config = new Config<WriterPoolable>()
			.setAllocator(new TimeoutAllocator(sleepTime));
		pool = new BlazePool<>(config);

		WriterPoolOutputWriter<WriterBasedOutputWriter> outputWriter = new WriterPoolOutputWriter<>(target, pool, claimTime);

		outputWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());
	}

	@Test(expected = IOException.class)
	@Category(IntegrationTest.class)
	public void poolableIsReleasedOnIOException() throws Exception {
		doThrow(IOException.class).when(target).write(any(Writer.class), any(Server.class), any(Query.class), any(ImmutableList.class));

		WriterPoolOutputWriter<WriterBasedOutputWriter> outputWriter = new WriterPoolOutputWriter<>(target, pool, new Timeout(1, SECONDS));
		try {
			outputWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());
		} finally {
			verify(allocator, timeout(500)).deallocate(any(WriterPoolable.class));
		}
	}

	private class DummyAllocator implements Allocator<WriterPoolable> {
		@Override
		public WriterPoolable allocate(Slot slot) throws Exception {
			return new SocketPoolable(slot, null, writer, new NeverFlush());
		}

		@Override
		public void deallocate(WriterPoolable poolable) throws Exception {
			poolable.release();
		}
	}

	private class TimeoutAllocator implements Allocator<WriterPoolable> {

		private Timeout timeout;

		public TimeoutAllocator(Timeout timeout) {
			this.timeout = timeout;
		}

		@Override
		public WriterPoolable allocate(Slot slot) throws Exception {
			try {
				Thread.sleep(timeout.getUnit().toMillis(timeout.getTimeout()));
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			return new SocketPoolable(slot, null, writer, new NeverFlush());
		}

		@Override
		public void deallocate(WriterPoolable poolable) throws Exception {

		}
	}
}
