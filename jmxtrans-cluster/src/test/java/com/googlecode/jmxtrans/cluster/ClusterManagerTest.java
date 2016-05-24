package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.zookeeper.ZookeeperClusterService;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.kaching.platform.testing.AllowDNSResolution;
import com.kaching.platform.testing.AllowLocalFileAccess;
import com.kaching.platform.testing.AllowNetworkAccess;
import com.kaching.platform.testing.AllowNetworkListen;
import com.sun.javafx.geom.transform.TransformHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.SyncBuilder;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.assertj.core.util.Compatibility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.*;

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
        //client = CuratorFrameworkFactory.newClient("10.0.0.6:2181", new ExponentialBackoffRetry(1000,3));
        client = CuratorFrameworkFactory.newClient(testingServer.getConnectString(), new ExponentialBackoffRetry(1000,3));
        //client = CuratorFrameworkFactory.newClient("10.189.33.100:2181,10.189.33.101:2181,10.189.33.102:2181", new ExponentialBackoffRetry(1000,3));
        client.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        testingServer.stop();
    }

    @Test
    public void testDependencyInjection() throws Exception {
        ClusterService service = ClusterServiceFactory.createClusterService(
                createGoldenConfiguration(testingServer.getConnectString()));

        assertEquals(service.getClass(), ZookeeperClusterService.class);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testMisstypedDependeny() throws Exception{
        Configuration configuration = createGoldenConfiguration(testingServer.getConnectString());
        configuration.setProperty("provider.classname", ZookeeperClusterService.class.getName().substring(1));

        ClusterService service = ClusterServiceFactory.createClusterService(configuration);
        configuration.setProperty("provider.classname", ZookeeperClusterService.class.getName());
    }

    @Test
    public void testZookeeperConnectionStartup() throws Exception {
        ClusterService service = ClusterServiceFactory.createClusterService(
                createGoldenConfiguration(testingServer.getConnectString()));
        service.startService();

        synchronized((ZookeeperClusterService)service){
            try{
                (service).wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        Stat stat = client.checkExists().forPath("/jmxtrans/workers/worker_01");
        assertThat(stat).isNotEqualTo(null);

        service.stopService();
    }

    @Test
    public void testConfigUpdate() throws Exception{

        client.create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/jmxtrans/jvms/jvm_01/config", "Config01".getBytes());
        client.create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/jmxtrans/jvms/jvm_01/affinity", "worker_01".getBytes());
        client.create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/jmxtrans/jvms/jvm_01/owner");
        client.create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/jmxtrans/jvms/jvm_02/config", "Config02".getBytes());
        client.create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/jmxtrans/jvms/jvm_02/affinity", "worker_02".getBytes());
        client.create().creatingParentContainersIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/jmxtrans/jvms/jvm_02/owner");
        Thread.sleep(3000);

        ClusterService service = ClusterServiceFactory.createClusterService(
                createGoldenConfiguration(testingServer.getConnectString()));

        service.startService();

        synchronized((ZookeeperClusterService)service){
            try{
                (service).wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        client.setData().forPath("/jmxtrans/jvms/jvm_01/config", "Config01 Update".getBytes());
        service.stopService();
    }

    @Test
    public void testNodeCache() throws Exception{
        NodeCache cache= new NodeCache(client, "/test/node");
        cache.start(true);

        client.create().forPath("/test/node", "a".getBytes());
        Thread.sleep(1000);
        cache.getListenable().addListener (new NodeCacheListener()
                {
                    @Override
                    public void nodeChanged() throws Exception
                    {
                        System.out.println("Node Changed" + client.checkExists().forPath("/test/node"));
                    }
                }
        );

        client.setData().forPath("/test/node", "b".getBytes());
        Thread.sleep(20000);

        client.close();

    }

    @Test
    public void testEmptyNode() throws Exception{
        client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/test/tnode", "Test data".getBytes());

        Thread.sleep(1000);

        byte[] data = client.getData().forPath("/test/tnode");
        System.out.println(new String(data));
        assertNotNull(data);

    }

    private Configuration createGoldenConfiguration(String connectionString){
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