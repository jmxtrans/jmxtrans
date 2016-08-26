package com.googlecode.jmxtrans.cluster.events;

import lombok.ToString;
import org.apache.curator.framework.state.ConnectionState;

import javax.annotation.Nonnull;

/**
 * ClusterStateChangeEvent. This class contains an event for the ClusterStateChangeListeners that are registered
 * in the ClusterService
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
@ToString(includeFieldNames=true)
public class ClusterStateChangeEvent {

    private final ConnectionState type;

    public ClusterStateChangeEvent(@Nonnull ConnectionState type) {
        this.type = type;
    }

    public ConnectionState getType() {
        return this.type;
    }
}
