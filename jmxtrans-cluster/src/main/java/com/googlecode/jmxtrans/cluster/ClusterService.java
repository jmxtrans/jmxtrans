package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.events.ClusterStateChangeListener;
import com.googlecode.jmxtrans.cluster.events.ConfigurationChangeListener;

import javax.annotation.Nonnull;


/**
 * ClusterService. It should be implemeted by any cluster provider.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public interface ClusterService {

    void startService();
    void stopService();
    void registerStateChangeListener(@Nonnull ClusterStateChangeListener stateChangeListener);
    void unregisterStateChangeListener(@Nonnull ClusterStateChangeListener stateChangeListener);
    void registerConfigurationChangeListener(@Nonnull ConfigurationChangeListener configurationChangeListener);
    void unregisterConfigurationChangeListener(@Nonnull ConfigurationChangeListener configurationChangeListener);
}
