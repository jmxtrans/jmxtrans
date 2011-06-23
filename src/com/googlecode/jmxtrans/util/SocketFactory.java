package com.googlecode.jmxtrans.util;

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
    public SocketFactory() {}

    /**
     * Creates the socket and the writer to go with it.
     */
    @Override
    public Object makeObject(Object key) throws Exception {
        SocketDetails details = (SocketDetails) key;
        Socket socket = new Socket(details.getHost(), details.getPort());
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
        return socket.isBound() && ! socket.isClosed() && socket.isConnected() && ! socket.isInputShutdown() && ! socket.isOutputShutdown();
    }
}
