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

import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.test.TCPEchoServer;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import stormpot.Slot;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Category(RequiresIO.class)
public class SocketAllocatorTest {

	@Category(IntegrationTest.class)
	@Test
	public void addressResolutionIsAlwaysDone() throws Exception {
		TCPEchoServer echoServer = new TCPEchoServer();
		try {
			echoServer.start();

			SocketAllocator socketAllocator = new SocketAllocator(echoServer.getLocalSocketAddress(), 500, UTF_8, new NeverFlush());
			SocketPoolable socketPoolable = socketAllocator.allocate(mock(Slot.class));

			try {
				InetSocketAddress remoteSocketAddress = (InetSocketAddress) socketPoolable.getSocket().getRemoteSocketAddress();
				assertThat(remoteSocketAddress).isEqualTo(echoServer.getLocalSocketAddress());
				// FIXME: the following test is not actually good. Not really sure how to validate that DNS resolution is done.
				assertThat(remoteSocketAddress).isNotSameAs(echoServer.getLocalSocketAddress());
			} finally {
				socketPoolable.getSocket().close();
			}
		} finally {
			echoServer.stop();
		}
	}

	@Test
	public void socketAndWritersAreClosed() throws Exception {
		SocketAllocator socketAllocator = new SocketAllocator(new InetSocketAddress("localhost", 80), 100, UTF_8, new NeverFlush());

		Socket socket = mock(Socket.class);
		Writer writer = mock(Writer.class);
		SocketPoolable socketPoolable = new SocketPoolable(null, socket, writer, new NeverFlush());
		socketAllocator.deallocate(socketPoolable);

		verify(socket).close();
		verify(writer).close();
	}

	@Test(expected = IOException.class)
	public void socketAndWritersAreClosedEvenWhenExceptions() throws Exception {
		SocketAllocator socketAllocator = new SocketAllocator(new InetSocketAddress("localhost", 80), 100, UTF_8, new NeverFlush());

		Socket socket = mock(Socket.class);
		doThrow(IOException.class).when(socket).close();
		Writer writer = mock(Writer.class);
		doThrow(IOException.class).when(writer).close();

		SocketPoolable socketPoolable = new SocketPoolable(null, socket, writer, new NeverFlush());
		socketAllocator.deallocate(socketPoolable);

		verify(socket).close();
		verify(writer).close();
	}

}
