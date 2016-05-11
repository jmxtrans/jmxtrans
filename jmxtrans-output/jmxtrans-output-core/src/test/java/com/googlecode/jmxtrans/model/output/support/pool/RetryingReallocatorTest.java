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
package com.googlecode.jmxtrans.model.output.support.pool;

import com.github.rholder.retry.RetryException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import stormpot.Allocator;
import stormpot.Poolable;
import stormpot.Slot;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

public class RetryingReallocatorTest {

	private Allocator<TestPoolable> allocator;
	private RetryingReallocator<TestPoolable> retryingReallocator;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		allocator = (Allocator<TestPoolable>) Mockito.mock(Allocator.class);
		retryingReallocator = new RetryingReallocator<TestPoolable>(allocator, 100, 5000);
	}

	@Test
	public void testAllocate() throws Exception {
		TestPoolable value = new TestPoolable("value");
		when(allocator.allocate(any(Slot.class))).thenReturn(value);

		Assert.assertSame(value, retryingReallocator.allocate(null));
	}

	@Test
	public void testDellocate() throws Exception {
		TestPoolable value = new TestPoolable("value");

		retryingReallocator.deallocate(value);
		Mockito.verify(allocator).deallocate(value);
	}

	@Test
	public void testReallocate() throws Exception {
		TestPoolable original = new TestPoolable("original");
		TestPoolable value = new TestPoolable("value");
		when(allocator.allocate(any(Slot.class))).thenReturn(value);

		Assert.assertSame(value, retryingReallocator.reallocate(null, original));
	}

	@Test
	public void testReallocate_FailThenSucceed() throws Exception {
		TestPoolable original = new TestPoolable("original");
		TestPoolable value = new TestPoolable("value");

		when(allocator.allocate(any(Slot.class)))
			.thenThrow(new IOException())
			.thenThrow(new IOException())
			.thenThrow(new IOException())
			.thenReturn(value);

		Assert.assertSame(value, retryingReallocator.reallocate(null, original));
		Mockito.verify(allocator, atLeast(2)).allocate(any(Slot.class));
	}

	@Test(expected = RetryException.class)
	public void testReallocate_Fail() throws Exception {
		TestPoolable original = new TestPoolable("original");

		when(allocator.allocate(any(Slot.class))).thenThrow(new IOException());

		retryingReallocator.reallocate(null, original);
	}

	private static class TestPoolable implements Poolable {

		public String value;

		public TestPoolable(String value) {
			this.value = value;
		}

		@Override
		public void release() {

		}
	}
}
