package com.googlecode.jmxtrans.cluster;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration.Configuration;

import javax.annotation.Nonnull;

/**
 * ClusterServiceFactory. This factory hides the guice dependecy injection. It creates a ClusterService based on the
 * classname defined in the configuration.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
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
