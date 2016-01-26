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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

@ThreadSafe
public class ChannelWriter extends Writer {
	@Nonnull private final Charset charset;
	@Nonnull private final ByteBuffer buffer;
	@Nonnull private final WritableByteChannel channel;

	public ChannelWriter(
			int bufferSize,
			@Nonnull Charset charset,
			@Nonnull WritableByteChannel channel) {
		this.charset = charset;
		this.channel = channel;
		buffer = ByteBuffer.allocate(bufferSize);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		synchronized (lock) {
			byte[] bytes = new String(cbuf, off, len).getBytes(charset);

			if (buffer.remaining() < (bytes.length + 1)) flush();

			buffer.put(bytes, off, len);
		}
	}

	@Override
	public void flush() throws IOException {
		synchronized (lock) {
			final int sizeOfBuffer = buffer.position();

			// empty buffer
			if (sizeOfBuffer <= 0) return;

			// send and reset the buffer
			buffer.flip();
			channel.write(buffer);
			buffer.limit(buffer.capacity());
			buffer.rewind();
		}
	}

	@Override
	public void close() throws IOException {
		Closer closer = Closer.create();
		try {
			closer.register(channel);
			flush();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}
}
