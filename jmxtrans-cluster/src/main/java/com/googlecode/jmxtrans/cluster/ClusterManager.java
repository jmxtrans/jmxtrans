package com.googlecode.jmxtrans.cluster;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * ClusterManager Tester.
 *
 * @author Tibor Kulcsar
 * @version 1.0
 * @since <pre>May 17, 2016</pre>
 */
public class ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);
    @VisibleForTesting ClusterService service;

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
    private static Injector createInjector(@Nonnull Configuration configuration) {
        return Guice.createInjector(
                new ClusterServiceModule(configuration));
    }

    @Nonnull
    public static ClusterManager createClusterManager(@Nonnull Configuration configuration){
        Injector injector = createInjector(configuration);
        return injector.getInstance(ClusterManager.class);
    }
}
