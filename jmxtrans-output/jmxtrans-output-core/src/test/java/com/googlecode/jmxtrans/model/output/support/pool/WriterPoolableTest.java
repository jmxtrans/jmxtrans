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
package com.googlecode.jmxtrans.model.output.support.pool;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import stormpot.Allocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Slot;
import stormpot.Timeout;

import javax.annotation.Nonnull;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WriterPoolableTest {

	@Mock private Writer writer;

	@Test
	public void writerIsFlushedIfStrategyRequiresIt() throws InterruptedException, IOException {
		BlazePool<WriterPoolable> pool = createPool();

		WriterPoolable writerPoolable = pool.claim(new Timeout(1, SECONDS));
		writerPoolable.getWriter().write("some message");
		writerPoolable.release();

		verify(writer).flush();
	}

	@Test
	public void ensureSlotIsReleasedOnException() throws InterruptedException, IOException {
		BlazePool<WriterPoolable> pool = createPool(new ExceptionOnFlush());

		WriterPoolable writerPoolable = pool.claim(new Timeout(1, SECONDS));
		writerPoolable.getWriter().write("some message");
		writerPoolable.release();

		// Slot should be able to be reclaimed
		writerPoolable = pool.claim(new Timeout(1, SECONDS));

		assertNotNull(writerPoolable);

	}

	private BlazePool<WriterPoolable> createPool() {
		return createPool(new AlwaysFlush());
	}

	private BlazePool<WriterPoolable> createPool(final FlushStrategy flushStrategy) {
		Config<WriterPoolable> config = new Config<>()
				.setAllocator(new Allocator<WriterPoolable>() {
					@Override
					public WriterPoolable allocate(Slot slot) throws Exception {
						return new WriterPoolable(slot, writer, flushStrategy);
					}

					@Override
					public void deallocate(WriterPoolable poolable) throws Exception {}
				})
				.setSize(1);
		return new BlazePool<>(config);
	}

	private class ExceptionOnFlush implements FlushStrategy {
		@Override
		public void flush(@Nonnull Flushable flushable) throws IOException {
			throw new IOException();
		}
	}

}
