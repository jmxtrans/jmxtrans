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

import com.google.common.annotations.VisibleForTesting;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.pool.WriterPoolable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.LifecycledPool;
import stormpot.Timeout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class WriterPoolOutputWriter<T extends WriterBasedOutputWriter> extends OutputWriterAdapter{

	private static final Logger logger = LoggerFactory.getLogger(WriterPoolOutputWriter.class);

	@Nonnull private final T target;
	@Nonnull private final LifecycledPool<? extends WriterPoolable> writerPool;
	@Nonnull private final Timeout poolClaimTimeout;
	@Nullable private int socketTimeoutMs;

	public WriterPoolOutputWriter(@Nonnull T target, @Nonnull LifecycledPool<? extends WriterPoolable> writerPool, @Nonnull Timeout poolClaimTimeout) {
		this.target = target;
		this.writerPool = writerPool;
		this.poolClaimTimeout = poolClaimTimeout;
	}

	public WriterPoolOutputWriter(@Nonnull T target, @Nonnull LifecycledPool<? extends WriterPoolable> writerPool, @Nonnull Timeout poolClaimTimeout, @Nullable int socketTimeoutMs) {
		this.target = target;
		this.writerPool = writerPool;
		this.poolClaimTimeout = poolClaimTimeout;
		this.socketTimeoutMs = socketTimeoutMs;
	}


	@Override
	public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
		WriterPoolable writerPoolable = claimWriter();
		try {
			target.write(writerPoolable.getWriter(), server, query, results);
		} catch (IOException ioe) {
			writerPoolable.invalidate();
			throw ioe;
		} finally {
			writerPoolable.release();
		}

	}

	@Override
	public void close() throws LifecycleException {
		writerPool.shutdown();
	}

	private WriterPoolable claimWriter() {
		WriterPoolable result = null;

		try {
			result = writerPool.claim(poolClaimTimeout);
		} catch (InterruptedException ex) {
			logger.error("Interrupted while attempting to claim writer from pool", ex);
			Thread.currentThread().interrupt();
		}

		if (result == null) {
			throw new IllegalStateException("Could not get writer from pool, please check if the server is available");
		}

		return result;
	}

	@VisibleForTesting
	public int getSocketTimeoutMs() {
		return socketTimeoutMs;
	}

	@VisibleForTesting
	Timeout getPoolClaimTimeout() {
		return poolClaimTimeout;
	}

	@VisibleForTesting
	public LifecycledPool<? extends WriterPoolable> getWriterPool() {
		return writerPool;
	}
}
