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
import org.mockito.runners.MockitoJUnitRunner;
import stormpot.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
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

	private BlazePool<WriterPoolable> createPool() {
		Config<WriterPoolable> config = new Config<>()
				.setAllocator(new Allocator<WriterPoolable>() {
					@Override
					public WriterPoolable allocate(Slot slot) throws Exception {
						return new WriterPoolable(slot, writer, new AlwaysFlush());
					}

					@Override
					public void deallocate(WriterPoolable poolable) throws Exception {}
				})
				.setSize(1);
		return new BlazePool<>(config);
	}

}
