package com.googlecode.jmxtrans.cluster;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration.Configuration;

import javax.annotation.Nonnull;

/**
 * Created by kulcsart on 5/20/2016.
 *
 */
public class ClusterServiceFactory {

    @Nonnull
    private static Injector createInjector(@Nonnull Configuration configuration) throws ClassNotFoundException {
        return Guice.createInjector(
                new ClusterServiceModule(configuration));
    }

    @Nonnull
    public static ClusterService createClusterService(@Nonnull Configuration configuration) throws ClassNotFoundException{
        Injector injector = createInjector(configuration);
        return injector.getInstance(ClusterService.class);
    }
}
