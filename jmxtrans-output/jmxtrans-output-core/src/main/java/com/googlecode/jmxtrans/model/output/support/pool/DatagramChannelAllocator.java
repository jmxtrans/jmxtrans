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

import com.google.common.io.Closer;
import stormpot.Allocator;
import stormpot.Slot;

import javax.annotation.Nonnull;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;

public class DatagramChannelAllocator implements Allocator<DatagramChannelPoolable> {

	@Nonnull private final InetSocketAddress server;
	private final int bufferSize;
	@Nonnull private final Charset charset;
	@Nonnull private final FlushStrategy flushStrategy;

	public DatagramChannelAllocator(
			@Nonnull InetSocketAddress server,
			int bufferSize,
			@Nonnull Charset charset,
			@Nonnull FlushStrategy flushStrategy) {
		this.server = server;
		this.bufferSize = bufferSize;
		this.charset = charset;
		this.flushStrategy = flushStrategy;
	}

	@Override
	public DatagramChannelPoolable allocate(Slot slot) throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.connect(new InetSocketAddress(server.getHostName(), server.getPort()));
		ChannelWriter writer = new ChannelWriter(bufferSize, charset, channel);
		return new DatagramChannelPoolable(slot, writer, channel, flushStrategy);
	}

	@Override
	public void deallocate(DatagramChannelPoolable poolable) throws Exception {
		Closer closer = Closer.create();
		try {
			Writer writer = closer.register(poolable.getWriter());
			writer.flush();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}
}
