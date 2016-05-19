package com.googlecode.jmxtrans.cluster;

/**
 * Created by kulcsart on 5/19/2016.
 *
 */
public class ClusterStateChangeEvent {

    private final ClusterStateChangeEvent.Type type;

    public ClusterStateChangeEvent(ClusterStateChangeEvent.Type type) {
        this.type = type;
    }

    public ClusterStateChangeEvent.Type getType() {
        return this.type;
    }

    public String toString() {
        return "PathChildrenCacheEvent{type=" + this.type + '}';
    }

    public static enum Type {
        CONNECTION_SUSPENDED,
        CONNECTION_RECONNECTED,
        CONNECTION_LOST,
        INITIALIZED;

        private Type() {
        }
    }
}
