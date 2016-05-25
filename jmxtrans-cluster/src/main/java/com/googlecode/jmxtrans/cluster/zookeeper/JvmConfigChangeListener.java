package com.googlecode.jmxtrans.cluster.zookeeper;

/**
 * JvmConfigChangeListener. This interface should be implemented by the Clusterserveice that is having the
 * JvmHandleres
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public interface JvmConfigChangeListener {
    void jvmConfigChanged(String jvmAlias, String jvmConfig);
    void jvmConfigRemoved(String jvmAlias);
}
