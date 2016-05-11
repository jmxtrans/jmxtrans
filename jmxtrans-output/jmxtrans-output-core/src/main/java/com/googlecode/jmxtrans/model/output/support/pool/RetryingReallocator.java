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

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Allocator;
import stormpot.Poolable;
import stormpot.Reallocator;
import stormpot.Slot;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RetryingReallocator<V extends Poolable> implements Reallocator<V> {

	private static final Logger log = LoggerFactory.getLogger(RetryingReallocator.class);

	/**
	 * Interval to wait between reallocation attempts
	 */
	private static final long WAIT_INTERVAL_MS = 100;

	/**
	 * Max time to wait when attempting to reallocate a slot
	 */
	private static final long MAX_WAIT_MS = TimeUnit.SECONDS.toMillis(60);

	@Nonnull private final Allocator<V> allocator;
	@Nonnull private final Retryer<V> retryer;

	public RetryingReallocator(Allocator<V> allocator) {
		this(allocator, WAIT_INTERVAL_MS, MAX_WAIT_MS);
	}

	/**
	 * @param allocator Allocator
	 * @param waitInterval Wait time between reallocation attempts (ms)
	 * @param maxWaitInterval Max time to wait for a reallocation (ms)
	 */
	public RetryingReallocator(Allocator<V> allocator, long waitInterval, long maxWaitInterval) {
		this.allocator = allocator;
		this.retryer = RetryerBuilder.<V>newBuilder()
			.retryIfException()
			.withWaitStrategy(WaitStrategies.fixedWait(waitInterval, TimeUnit.MILLISECONDS))
			.withStopStrategy(StopStrategies.stopAfterDelay(maxWaitInterval, TimeUnit.MILLISECONDS))
			.withRetryListener(new RetryListener() {
				@Override
				public <V> void onRetry(Attempt<V> attempt) {
					if (attempt.hasException()) {
						log.error("Error allocating slot", attempt.getExceptionCause());
					}
				}
			})
			.build();
	}

	@Override
	public V reallocate(final Slot slot, V poolable) throws Exception {
		try {
			deallocate(poolable);
		} catch (Throwable ignore) { // NOPMD
			// ignored as per specification
		}

		return retryer.call(new Callable<V>() {
			@Override
			public V call() throws Exception {
				return allocate(slot);
			}
		});
	}

	@Override
	public V allocate(Slot slot) throws Exception {
		return allocator.allocate(slot);
	}

	@Override
	public void deallocate(V poolable) throws Exception {
		allocator.deallocate(poolable);
	}
}
