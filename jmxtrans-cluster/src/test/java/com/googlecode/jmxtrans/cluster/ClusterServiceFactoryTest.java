package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.zookeeper.ZookeeperClusterService;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * ClusterServiceFactory Tester.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 25, 2016</pre>
 */
public class ClusterServiceFactoryTest {

    /**
     * Test the classname based dependecy injection.
     * @throws Exception
     */
    @Test
    public void testDependencyInjection() throws Exception {
        ClusterService service = ClusterServiceFactory.createClusterService(
                ConfigurationFixtures.createGoldenConfiguration("127.0.0.1:2181"));

        assertEquals(service.getClass(), ZookeeperClusterService.class);
    }

    /**
     * Check if the defined class is not availabel in the classpath.
     * @throws Exception
     */
    @Test(expected = ClassNotFoundException.class)
    public void testMisstypedDependeny() throws Exception{
        Configuration configuration =  ConfigurationFixtures.createGoldenConfiguration("127.0.0.1:2181");
        configuration.setProperty("provider.classname", ZookeeperClusterService.class.getName().substring(1));

        ClusterService service = ClusterServiceFactory.createClusterService(configuration);
    }
}
