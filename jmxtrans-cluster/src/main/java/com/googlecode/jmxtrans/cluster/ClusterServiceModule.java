package com.googlecode.jmxtrans.cluster;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ClusterServiceModule. Google Guice dependency injection module.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ClusterServiceModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(ClusterServiceModule.class);
    Class implClass;
    Configuration configuration;

    public ClusterServiceModule(Configuration configuration) throws ClassNotFoundException{
        this.configuration = configuration;
        checkNotNull(configuration);
        implClass = getImplClassForName(configuration.getString("provider.classname"));
    }

    @Override
    protected void configure() {
        bind(ClusterService.class).to(implClass);
    }

    @Provides
    Configuration clusterConfiguration(){return this.configuration;}

    private Class getImplClassForName(String className) throws ClassNotFoundException{
            return Class.forName(className);

    }
}
