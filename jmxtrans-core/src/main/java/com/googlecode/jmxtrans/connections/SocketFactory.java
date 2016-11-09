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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Allows us to pool socket connections.
 */
public class SocketFactory extends BaseKeyedPooledObjectFactory<InetSocketAddress, Socket> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public Socket create(InetSocketAddress address) throws Exception {
		Socket socket = new Socket(address.getHostName(), address.getPort());
		socket.setKeepAlive(true);
		return socket;
	}

	@Override
	public PooledObject<Socket> wrap(Socket socket) {
		return new DefaultPooledObject<>(socket);
	}

	@Override
	public void destroyObject(InetSocketAddress key, PooledObject<Socket> p) throws Exception {
		p.getObject().close();
	}

	@Override
	public boolean validateObject(InetSocketAddress address, PooledObject<Socket> p) {
		Socket socket = p.getObject();

		if (socket == null) {
			log.error("Socket is null [{}]", address);
			return false;
		}

		if (!socket.isBound()) {
			log.error("Socket is not bound [{}]", address);
			return false;
		}
		if (socket.isClosed()) {
			log.error("Socket is closed [{}]", address);
			return false;
		}
		if (!socket.isConnected()) {
			log.error("Socket is not connected [{}]", address);
			return false;
		}
		if (socket.isInputShutdown()) {
			log.error("Socket input is shutdown [{}]", address);
			return false;
		}
		if (socket.isOutputShutdown()) {
			log.error("Socket output is shutdown [{}]", address);
			return false;
		}
		return true;
	}

}
