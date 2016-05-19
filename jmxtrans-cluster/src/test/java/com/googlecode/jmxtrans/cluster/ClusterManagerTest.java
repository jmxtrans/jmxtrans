package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.zookeeper.ZookeeperClusterService;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.kaching.platform.testing.AllowDNSResolution;
import com.kaching.platform.testing.AllowLocalFileAccess;
import com.kaching.platform.testing.AllowNetworkAccess;
import com.kaching.platform.testing.AllowNetworkListen;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.data.Stat;
import org.junit.*;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/**
 * ClusterManager Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>May 14, 2016</pre>
 */
@Category({IntegrationTest.class, RequiresIO.class})
@AllowDNSResolution
@AllowNetworkListen(ports = {0})
@AllowNetworkAccess(endpoints = {"127.0.0.1:*"})
@AllowLocalFileAccess(paths = {"*"})
public class ClusterManagerTest {

    private static TestingServer testingServer;
    private static CuratorFramework client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        testingServer = new TestingServer(2181,true);
        client = CuratorFrameworkFactory.newClient(testingServer.getConnectString(), new ExponentialBackoffRetry(1000,3));
        client.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        testingServer.stop();
    }

    @Test
    public void testDependencyInjection() throws Exception {
        ClusterManager manager = ClusterManager.createClusterManager(createGoldenConfiguration(testingServer.getConnectString()));
        assertTrue(ZookeeperClusterService.class.getName().equals(manager.service.getClass().getName()));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testMisstypedDependeny() throws Exception{
        Configuration configuration = createGoldenConfiguration(testingServer.getConnectString());
        configuration.setProperty("provider.classname", ZookeeperClusterService.class.getName().substring(1));
        ClusterManager manager = ClusterManager.createClusterManager(configuration);

        assertTrue(ZookeeperClusterService.class.getName().equals(manager.service.getClass().getName()));
        configuration.setProperty("provider.classname", ZookeeperClusterService.class.getName());
    }

    @Test
    public void testZookeeperConnectionStartup() throws Exception {
        Configuration configuration = createGoldenConfiguration(testingServer.getConnectString());
        ClusterManager manager = ClusterManager.createClusterManager(configuration);

        manager.start();

        synchronized((ZookeeperClusterService)manager.service){
            try{
                (manager.service).wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        Stat stat = client.checkExists().forPath("/jmxtrans/workers/worker_01");
        assertFalse(null == stat);

        manager.stop();
    }

    private Configuration createGoldenConfiguration(String connectionString){
        Configuration configuration = new HierarchicalConfiguration();
        configuration.addProperty("provider.classname", ZookeeperClusterService.class.getName());
        configuration.addProperty("zookeeper.workeralias", "worker_01");
        configuration.addProperty("zookeeper.connectionstring", connectionString);
        configuration.addProperty("zookeeper.timeout", 1000);
        configuration.addProperty("zookeeper.retry", 3);
        configuration.addProperty("zookeeper.heartbeatpath", "/jmxtrans/workers");
        configuration.addProperty("zookeeper.configpath", "/jmxtrans/jvms");
        return  configuration;
    }



}