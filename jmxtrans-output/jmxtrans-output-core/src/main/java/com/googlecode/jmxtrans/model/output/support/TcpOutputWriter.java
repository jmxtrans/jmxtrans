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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.pool.SocketAllocator;
import com.googlecode.jmxtrans.model.output.support.pool.SocketExpiration;
import com.googlecode.jmxtrans.model.output.support.pool.SocketPoolable;
import lombok.Setter;
import lombok.experimental.Accessors;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.LifecycledPool;
import stormpot.Timeout;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TcpOutputWriter<T extends WriterBasedOutputWriter> extends OutputWriterAdapter{

	@Nonnull private final T target;
	@Nonnull private final LifecycledPool<SocketPoolable> socketPool;

	public TcpOutputWriter(@Nonnull T target, @Nonnull LifecycledPool<SocketPoolable> socketPool) {
		this.target = target;
		this.socketPool = socketPool;
	}

	@Override
	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		try {
			SocketPoolable socketPoolable = socketPool.claim(new Timeout(1, SECONDS));
			try {
				target.write(socketPoolable.getWriter(), server, query, results);
			} catch (IOException ioe) {
				socketPoolable.invalidate();
				throw ioe;
			} finally {
				socketPoolable.release();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Could not get socket from pool, please check is the server is available");
		}
	}

	@Override
	public void stop() throws LifecycleException {
		socketPool.shutdown();
	}

	public static <T extends WriterBasedOutputWriter> Builder<T> builder(
			@Nonnull InetSocketAddress server,
			@Nonnull T target) {
		return new Builder<T>(server, target);
	}

	@Accessors(chain = true)
	public static class Builder<T extends WriterBasedOutputWriter> {
		@Nonnull private final InetSocketAddress server;
		@Nonnull private final T target;
		@Nonnull @Setter private Charset charset = Charsets.UTF_8;
		@Setter private int socketTimeoutMillis = 200;
		@Setter private int poolSize = 1;

		public Builder(@Nonnull InetSocketAddress server, @Nonnull T target) {
			this.server = server;
			this.target = target;
		}

		public TcpOutputWriter<T> build() {
			Config<SocketPoolable> config = new Config<SocketPoolable>()
					.setAllocator(new SocketAllocator(
							server,
							socketTimeoutMillis,
							charset))
					.setExpiration(new SocketExpiration())
					.setSize(poolSize);
			LifecycledPool<SocketPoolable> pool = new BlazePool<SocketPoolable>(config);
			return new TcpOutputWriter<T>(target, pool);
		}
	}
}
