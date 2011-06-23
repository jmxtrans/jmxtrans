package com.googlecode.jmxtrans.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Class which holds the details about the socket connection. Primarily host/port.
 */
public class SocketDetails implements java.io.Serializable {
    private String host;
    private int port;

    public SocketDetails(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static SocketDetails fromString(String key) {
        String[] bits = key.split(":");
        return new SocketDetails(bits[0], Integer.parseInt(bits[1]));
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
        boolean result;
        if (o == null) {
            result = false;
        }
        if (o == this) {
            result = true;
        }
        if (o.getClass() != this.getClass()) {
            result = false;
        }

        if (!(o instanceof SocketDetails)) {
            result = false;
        }

        SocketDetails other = (SocketDetails)o;

        result = new EqualsBuilder()
                            .append(this.getHost(), other.getHost())
                            .append(this.getPort(), other.getPort())
                            .isEquals();
        return result;
    }

    @Override
    public int hashCode() {
        int result = new HashCodeBuilder(21, 45)
                                    .append(this.getHost())
                                    .append(this.getPort())
                                    .toHashCode();

        return result;
    }

    @Override
    public String toString() {
        return this.host + ":" + this.port;
    }
}