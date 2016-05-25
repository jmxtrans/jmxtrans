package com.googlecode.jmxtrans.cluster;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ClusterServiceModule. Google Guice dependency injection module. The injection is defined in the configuration.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ClusterServiceModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(ClusterServiceModule.class);
    Class implClass;
    Configuration configuration;

    public ClusterServiceModule(Configuration configuration) throws ClassNotFoundException{
        this.configuration = checkNotNull(configuration);
        implClass = Class.forName(configuration.getString("provider.classname"));
    }

    @Override
    protected void configure() {
        bind(ClusterService.class).to(implClass);
    }

    @Provides
    Configuration getClusterConfiguration(){return this.configuration;}
}
