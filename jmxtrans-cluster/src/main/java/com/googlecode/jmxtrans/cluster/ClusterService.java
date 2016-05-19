package com.googlecode.jmxtrans.cluster;

/**
 * ClusterService. It should be implemeted by any cluster provider.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public interface ClusterService {

    void startService();
    void stopService();
}
