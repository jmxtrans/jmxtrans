package com.googlecode.jmxtrans.connections;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Allows us to pool socket connections.
 */
public class SocketFactory extends BaseKeyedPoolableObjectFactory<InetSocketAddress, Socket> {

	/**
	 * Creates the socket and the writer to go with it.
	 */
	@Override
	public Socket makeObject(InetSocketAddress address) throws Exception {
		Socket socket = new Socket(address.getHostName(), address.getPort());
		socket.setKeepAlive(true);
		return socket;
	}

	/**
	 * Closes the socket.
	 */
	@Override
	public void destroyObject(InetSocketAddress address, Socket socket) throws Exception {
		socket.close();
	}

	/**
	 * Validates that the socket is good.
	 */
	@Override
	public boolean validateObject(InetSocketAddress address, Socket socket) {
		try {
			socket.setSoTimeout(100);
			if (socket.getInputStream().read() == -1) {
				return false;
			}
		} catch (java.net.SocketTimeoutException e) {
		} catch (Exception e) {
			return false;
		}
		return socket.isBound() && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown();
	}
}
