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
package com.googlecode.jmxtrans.connections;

import org.junit.Test;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class DatagramSocketFactoryTest {
	@Test
	public void testDatagramSocketFactoryMakeObject() throws Exception {
		int port = 50123;

		DatagramSocketFactory socketFactory = new DatagramSocketFactory();

		InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);

		DatagramSocket socketObject = socketFactory.makeObject(socketAddress);

		// Test if the remote address/port is the correct one.
		assertThat(socketObject.getPort()).isEqualTo(port);
		assertThat(socketObject.getInetAddress()).isEqualTo(Inet4Address.getLocalHost());
	}
}
