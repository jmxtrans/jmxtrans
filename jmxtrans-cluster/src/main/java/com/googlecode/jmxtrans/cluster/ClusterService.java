package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.events.ClusterStateChangeListener;
import com.googlecode.jmxtrans.cluster.events.ConfigurationChangeListener;

/**
 * ClusterService. It should be implemeted by any cluster provider.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public interface ClusterService {

    void startService();
    void stopService();
    void registerStateChangeListener(ClusterStateChangeListener stateChangeListener);
    void unregisterStateChangeListener(ClusterStateChangeListener stateChangeListener);
    void registerConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener);
    void unregisterConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener);
}
