package com.googlecode.jmxtrans.cluster;

/**
 * Created by kulcsart on 5/19/2016.
 */
public class ConfigurationChangeEvent {
    private final ConfigurationChangeEvent.Type type;


    public ConfigurationChangeEvent(ConfigurationChangeEvent.Type type) {
        this.type = type;
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
