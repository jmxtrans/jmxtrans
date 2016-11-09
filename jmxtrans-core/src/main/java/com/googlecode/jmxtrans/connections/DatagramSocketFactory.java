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

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * Allows us to pool socket connections.
 */
public class DatagramSocketFactory extends BaseKeyedPooledObjectFactory<SocketAddress, DatagramSocket> {

	@Override
	public DatagramSocket create(SocketAddress socketAddress) throws Exception {
		DatagramSocket datagramSocket = new DatagramSocket();
		datagramSocket.connect(socketAddress);
		return datagramSocket;
	}

	@Override
	public PooledObject<DatagramSocket> wrap(DatagramSocket socket) {
		return new DefaultPooledObject<>(socket);
	}

	@Override
	public boolean validateObject(SocketAddress key, PooledObject<DatagramSocket> p) {
		DatagramSocket socket = p.getObject();
		return socket.isBound() && !socket.isClosed() && socket.isConnected();
	}

	@Override
	public void destroyObject(SocketAddress key, PooledObject<DatagramSocket> p) throws Exception {
		p.getObject().close();
	}
}
