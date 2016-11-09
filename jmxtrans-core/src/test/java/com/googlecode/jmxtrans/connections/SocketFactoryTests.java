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
package com.googlecode.jmxtrans.connections;

import com.google.common.io.Closer;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.test.TCPEchoServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresIO.class)
public class SocketFactoryTests {

	@Rule
	public TCPEchoServer echoServer = new TCPEchoServer();

	@Test
	public void createdSocketIsValid() throws IOException {
		Closer closer = Closer.create();
		try {
			SocketFactory socketFactory = new SocketFactory();
			Socket socket = closer.register(socketFactory.makeObject(getServerAddress()));
			assertThat(socketFactory.validateObject(getServerAddress(), socket)).isTrue();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@Test
	public void nullSocketIsInvalid() {
		assertThat(new SocketFactory().validateObject(getServerAddress(), null)).isFalse();
	}

	@Test
	public void closedSocketIsInvalid() throws IOException {
		Closer closer = Closer.create();
		try {
			SocketFactory socketFactory = new SocketFactory();
			Socket socket = closer.register(socketFactory.makeObject(getServerAddress()));
			socket.close();
			assertThat(socketFactory.validateObject(getServerAddress(), socket)).isFalse();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	private InetSocketAddress getServerAddress() {
		return echoServer.getLocalSocketAddress();
	}

}
