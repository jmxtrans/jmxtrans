package com.googlecode.jmxtrans.connections;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Allows us to pool socket connections.
 */
public class SocketFactory extends BaseKeyedPoolableObjectFactory<InetSocketAddress, Socket> {

	private final Logger log = LoggerFactory.getLogger(getClass());

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
