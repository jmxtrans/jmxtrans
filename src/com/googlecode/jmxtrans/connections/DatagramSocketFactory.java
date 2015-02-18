package com.googlecode.jmxtrans.connections;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;

import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * Allows us to pool socket connections.
 */
public class DatagramSocketFactory extends BaseKeyedPoolableObjectFactory<SocketAddress, DatagramSocket> {

	/**
	 * Creates the socket and the writer to go with it.
	 */
	@Override
	public DatagramSocket makeObject(SocketAddress socketAddress) throws Exception {
		DatagramSocket datagramSocket = new DatagramSocket();
		datagramSocket.connect(socketAddress);
		return datagramSocket;
	}

	/**
	 * Closes the socket.
	 */
	@Override
	public void destroyObject(SocketAddress key, DatagramSocket socket) throws Exception {
		socket.close();
	}

	/**
	 * Validates that the socket is good.
	 */
	@Override
	public boolean validateObject(SocketAddress key, DatagramSocket socket) {
		return socket.isBound() && !socket.isClosed() && socket.isConnected();
	}
}
