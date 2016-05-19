package com.googlecode.jmxtrans.cluster;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ConfigurationChangeEvent. This class is passed to all the registered ClusterStateChangeListener. It contains
 * the main events relating to the cluster connection.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ConfigurationChangeEvent {
    private final ConfigurationChangeEvent.Type type;

    private final String configuration;


    public ConfigurationChangeEvent(ConfigurationChangeEvent.Type type, String configuration) {
        this.type = checkNotNull(type, "The change type cannot be null!");
        this.configuration = checkNotNull(configuration, "The configuration cannot be null!");
    }

    public ConfigurationChangeEvent.Type getType() {
        return this.type;
    }

    public String toString() {
        return "PathChildrenCacheEvent{type=" + this.type + '}';
    }

    public static enum Type {
        JVM_CONFIGURATION_ADDED,
        JVM_CONFIGURATION_REMOVED,
        JVM_CONFIGURATION_CHANGED;

        private Type() {
        }
    }

}
