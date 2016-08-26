package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.zookeeper.ZookeeperClusterService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * ConfigurationFixtures. The place of common routines for cluster testing.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 25, 2016</pre>
 */
public class ConfigurationFixtures {

    public static Configuration createGoldenConfiguration(String connectionString){
        Configuration configuration = new HierarchicalConfiguration();
        configuration.addProperty("provider.classname", ZookeeperClusterService.class.getName());
        configuration.addProperty("zookeeper.workeralias", "worker_01");
        //configuration.addProperty("zookeeper.connectionstring", "10.189.33.100:2181\\,10.189.33.101:2181\\,10.189.33.102:2181");
        configuration.addProperty("zookeeper.connectionstring", connectionString);
        configuration.addProperty("zookeeper.timeout", 1000);
        configuration.addProperty("zookeeper.retry", 3);
        configuration.addProperty("zookeeper.heartbeatpath", "/jmxtrans/workers");
        configuration.addProperty("zookeeper.configpath", "/jmxtrans/jvms");
        return  configuration;
    }
}
