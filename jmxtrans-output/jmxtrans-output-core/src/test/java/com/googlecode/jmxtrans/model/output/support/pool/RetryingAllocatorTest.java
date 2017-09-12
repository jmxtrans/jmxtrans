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

import com.github.rholder.retry.RetryException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import stormpot.Allocator;
import stormpot.Poolable;
import stormpot.Slot;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RetryingAllocatorTest {

	@Mock private Allocator<TestPoolable> allocator;
	private RetryingAllocator<TestPoolable> retryingAllocator;
	@Mock private Slot slot;

	@Before
	public void setUp() {
		retryingAllocator = new RetryingAllocator<>(allocator, 100, 5000);
	}

	@Test
	public void testDeallocate() throws Exception {
		TestPoolable value = new TestPoolable("value");

		retryingAllocator.deallocate(value);
		Mockito.verify(allocator).deallocate(value);
	}

	@Test
	public void testAllocate() throws Exception {
		TestPoolable value = new TestPoolable("value");
		when(allocator.allocate(slot)).thenReturn(value);

		Assert.assertSame(value, retryingAllocator.allocate(slot));
	}

	@Test
	public void testAllocate_FailThenSucceed() throws Exception {
		TestPoolable value = new TestPoolable("value");

		when(allocator.allocate(slot))
			.thenThrow(new IOException())
			.thenThrow(new IOException())
			.thenThrow(new IOException())
			.thenReturn(value);

		Assert.assertSame(value, retryingAllocator.allocate(slot));
		Mockito.verify(allocator, atLeast(2)).allocate(slot);
	}

	@Test(expected = RetryException.class)
	public void testAllocate_Fail() throws Exception {
		when(allocator.allocate(slot)).thenThrow(new IOException());

		retryingAllocator.allocate(slot);
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
