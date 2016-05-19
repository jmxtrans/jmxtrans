package com.googlecode.jmxtrans.cluster;

/**
 * ClusterStateChangeListener. This interface should be implemented by the client that is using the
 * ClusterManager or ClusterService
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public interface ClusterStateChangeListener {
    void cluterStateChanged(ClusterStateChangeEvent changeEvent);
}
