package com.googlecode.jmxtrans.cluster.events;

import org.apache.curator.framework.state.ConnectionState;

/**
 * ClusterStateChangeEvent. This class contains an event for the ClusterStateChangeListeners that are registered
 * in the ClusterService
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ClusterStateChangeEvent {

    private final ConnectionState type;

    public ClusterStateChangeEvent(ConnectionState type) {
        this.type = type;
    }

    public ConnectionState getType() {
        return this.type;
    }

    public String toString() {
        return "PathChildrenCacheEvent{type=" + this.type + '}';
    }

}
