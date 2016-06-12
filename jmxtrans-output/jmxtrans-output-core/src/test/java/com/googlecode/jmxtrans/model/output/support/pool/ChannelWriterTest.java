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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ChannelWriterTest {

	@Test
	public void messageIspassedToChannel() throws IOException {
		MyByteChannel channel = new MyByteChannel(UTF_8);
		ChannelWriter writer = new ChannelWriter(20, UTF_8, channel);

		writer.write("hello world");
		writer.flush();

		assertThat(channel.toString()).isEqualTo("hello world");
	}

	@Test
	public void bufferIsFlushedOnClose() throws IOException {
		MyByteChannel channel = new MyByteChannel(UTF_8);
		ChannelWriter writer = new ChannelWriter(20, UTF_8, channel);

		writer.write("hello world");
		writer.close();

		assertThat(channel.toString()).isEqualTo("hello world");
	}

	@Test
	public void writerFlushesWhenBufferIsFull() throws IOException {
		MyByteChannel channel = new MyByteChannel(UTF_8);
		ChannelWriter writer = new ChannelWriter(20, UTF_8, channel);

		writer.write("hello world 1");
		writer.write("hello world 2"); // trigger the flush, but itself will not be written to channel

		assertThat(channel.toString()).isEqualTo("hello world 1");
	}


	@Test
	public void multipleMessagesAreSent() throws IOException {
		MyByteChannel channel = new MyByteChannel(UTF_8);
		ChannelWriter writer = new ChannelWriter(20, UTF_8, channel);

		writer.write("hello world 1");
		writer.write("hello world 2");
		writer.flush();

		assertThat(channel.toString()).isEqualTo("hello world 1" + "hello world 2");
	}

	private static class MyByteChannel implements WritableByteChannel {

		@Nonnull private final StringBuffer buffer = new StringBuffer();
		@Nonnull private final Charset charset;

		public MyByteChannel(Charset charset) {
			this.charset = charset;
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			byte[] bytes = new byte[src.remaining()];
			src.get(bytes);
			buffer.append(new String(bytes, charset));
			return 0;
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public String toString() {
			return buffer.toString();
		}
	}
}
