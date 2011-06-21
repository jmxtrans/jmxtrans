package com.googlecode.jmxtrans.util;

import java.net.Socket;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;

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
        Details details = (Details) key;
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

    /**
     * Class which holds the details about the socket connection. Primarily host/port.
     */
    public static class Details {
        private String host;
        private int port;

        public Details(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }
        public void setHost(String host) {
            this.host = host;
        }
        public int getPort() {
            return port;
        }
        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (o.getClass() != this.getClass()) {
                return false;
            }

            if (!(o instanceof Query)) {
                return false;
            }

            Details other = (Details)o;

            return new EqualsBuilder()
                                .append(this.getHost(), other.getHost())
                                .append(this.getPort(), other.getPort())
                                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(21, 45)
                                        .append(this.getHost())
                                        .append(this.getPort())
                                        .toHashCode();
        }
    }
}
