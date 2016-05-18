package com.googlecode.jmxtrans.cluster;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by kulcsart on 5/14/2016.
 *
 */
public class ClusterServiceModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(ClusterServiceModule.class);
    Class implClass;
    Configuration configuration;

    public ClusterServiceModule(Configuration configuration){
        this.configuration = configuration;

        checkNotNull(configuration);
        String className = configuration.getString("provider.classname");
        checkNotNull(className);

        implClass = getImplClassForName(className);
        checkNotNull(implClass);
    }

    @Override
    protected void configure() {
        bind(ClusterService.class).to(implClass);
    }

    @Provides
    Configuration clusterConfiguration(){return this.configuration;}

    private Class getImplClassForName(String className){
        try{
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("ClusterService implementation {} cannot be found in classpath!", className );
            return null;
        }
    }
}
