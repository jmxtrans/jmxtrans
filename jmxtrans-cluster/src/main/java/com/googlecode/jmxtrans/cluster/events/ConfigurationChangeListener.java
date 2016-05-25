package com.googlecode.jmxtrans.cluster.events;

/**
 * ConfigurationChangeListener. This interface should be implemented by the client that is using the ClusterService
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public interface ConfigurationChangeListener {
    void configurationChanged(ConfigurationChangeEvent changeEvent);
}
