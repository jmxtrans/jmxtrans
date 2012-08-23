package com.googlecode.jmxtrans.util;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows us to pool socket connections.
 */
public class SocketFactory extends BaseKeyedPoolableObjectFactory {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(SocketFactory.class);

	/** constructor */
	public SocketFactory() {
	}

	/**
	 * Creates the socket and the writer to go with it.
	 */
	@Override
	public Object makeObject(Object key) throws Exception {
		InetSocketAddress details = (InetSocketAddress) key;
		Socket socket = new Socket(details.getHostName(), details.getPort());
		socket.setKeepAlive(true);
		return socket;
	}

	/**
	 * Closes the socket.
	 */
	@Override
	public void destroyObject(Object key, Object obj) throws Exception {
		Socket socket = (Socket) obj;
		socket.close();
	}

	/**
	 * Validates that the socket is good.
	 */
	@Override
	public boolean validateObject(Object key, Object obj) {
		Socket socket = (Socket) obj;
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
