package com.googlecode.jmxtrans.cluster;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * ClusterManager. It wraps the cluster service class and add some logic to it. This class handles the registration of
 * notification listeners, and nitify then if something happens in the cluster service.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);
    @VisibleForTesting ClusterService service;

    private List<ClusterStateChangeListener> clusterStateChangeListeners = ImmutableList.of();
    private List<ConfigurationChangeListener> configurationChangeListeners = ImmutableList.of();

    @Inject
    public void setSetvive(ClusterService service){
        this.service = service;
    }

    public void start(){
        this.service.startService();
    }

    public void stop(){
        this.service.stopService();
    }

    @Nonnull
    private static Injector createInjector(@Nonnull Configuration configuration) throws ClassNotFoundException {
        return Guice.createInjector(
                new ClusterServiceModule(configuration));
    }

    @Nonnull
    public static ClusterManager createClusterManager(@Nonnull Configuration configuration) throws ClassNotFoundException{
        Injector injector = createInjector(configuration);
        return injector.getInstance(ClusterManager.class);
    }

    public String getJmxTransConfiguration(){
        return "";
    }

    public void registerStateChangeListener(ClusterStateChangeListener stateChangeListener){
        this.clusterStateChangeListeners.add(stateChangeListener);
    }
    public void unregisterStateChangeListener(ClusterStateChangeListener stateChangeListener){
        this.clusterStateChangeListeners.remove(stateChangeListener);
    }
    public void registerConfigurationChangeListeners(ConfigurationChangeListener configurationChangeListener){
        this.configurationChangeListeners.add(configurationChangeListener);
    }
    public void unregisterConfigurationChangeListeners(ConfigurationChangeListener configurationChangeListener){
        this.configurationChangeListeners.remove(configurationChangeListener);
    }
}
