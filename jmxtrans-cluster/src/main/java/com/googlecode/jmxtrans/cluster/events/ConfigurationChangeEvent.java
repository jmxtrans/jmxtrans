package com.googlecode.jmxtrans.cluster.events;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ConfigurationChangeEvent. This class is passed to all the registered ClusterStateChangeListener. It contains
 * the main events relating to the cluster connection.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
@ToString(includeFieldNames=true, exclude = "configuration")
public class ConfigurationChangeEvent {
    @Getter
    private final ConfigurationChangeEvent.Type type;

    @Getter
    private final String[] configuration;

    public ConfigurationChangeEvent(@Nonnull ConfigurationChangeEvent.Type type, @Nonnull String[] configuration) {
        this.type = type;
        this.configuration = configuration;
    }
    
    public static enum Type {
        JVM_CONFIGURATION_ADDED,
        JVM_CONFIGURATION_REMOVED,
        JVM_CONFIGURATION_CHANGED;

        private Type() {
        }
    }

}
