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

import com.google.common.base.Charsets;
import com.googlecode.jmxtrans.model.output.support.pool.FlushStrategy;
import com.googlecode.jmxtrans.model.output.support.pool.NeverFlush;
import com.googlecode.jmxtrans.model.output.support.pool.RetryingAllocator;
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
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static java.util.concurrent.TimeUnit.SECONDS;

@Accessors(chain = true)
public class TcpOutputWriterBuilder<T extends WriterBasedOutputWriter> {
	@Nonnull private final InetSocketAddress server;
	@Nonnull private final T target;
	@Nonnull @Setter private Charset charset = Charsets.UTF_8;
	@Setter private int socketTimeoutMillis = 200;
	@Setter private int poolClaimTimeoutSeconds = 1;
	@Setter private int poolSize = 1;
	@Nonnull @Setter private FlushStrategy flushStrategy = new NeverFlush();

	private TcpOutputWriterBuilder(@Nonnull InetSocketAddress server, @Nonnull T target) {
		this.server = server;
		this.target = target;
	}

	public static <T extends WriterBasedOutputWriter> TcpOutputWriterBuilder<T> builder(
			@Nonnull InetSocketAddress server,
			@Nonnull T target) {
		return new TcpOutputWriterBuilder<>(server, target);
	}

	private LifecycledPool<SocketPoolable> createPool() {
		Config<SocketPoolable> config = new Config<SocketPoolable>()
				.setAllocator(new RetryingAllocator<SocketPoolable>(new SocketAllocator(
						server,
						socketTimeoutMillis,
						charset,
						flushStrategy)))
				.setExpiration(new SocketExpiration())
				.setSize(poolSize);
		return new BlazePool<>(config);
	}

	public WriterPoolOutputWriter<T> build() {
		LifecycledPool<SocketPoolable> pool = createPool();
		return new WriterPoolOutputWriter<>(target, pool, new Timeout(poolClaimTimeoutSeconds, SECONDS), socketTimeoutMillis);
	}
}
