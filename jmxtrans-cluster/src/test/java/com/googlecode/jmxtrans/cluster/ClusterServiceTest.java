package com.googlecode.jmxtrans.cluster;

import com.googlecode.jmxtrans.cluster.zookeeper.ZookeeperClusterService;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.kaching.platform.testing.AllowDNSResolution;
import com.kaching.platform.testing.AllowLocalFileAccess;
import com.kaching.platform.testing.AllowNetworkAccess;
import com.kaching.platform.testing.AllowNetworkListen;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.*;

/**
 * ClusterService Tester.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 14, 2016</pre>
 */
@Category({IntegrationTest.class, RequiresIO.class})
@AllowDNSResolution
@AllowNetworkListen(ports = {0})
@AllowNetworkAccess(endpoints = {"127.0.0.1:*"})
@AllowLocalFileAccess(paths = {"*"})
public class ClusterServiceTest {

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
    public void testZookeeperConnectionStartup() throws Exception {
        ClusterService service = ClusterServiceFactory.createClusterService(
                ConfigurationFixtures.createGoldenConfiguration(testingServer.getConnectString()));
        service.startService();

        synchronized(service){
            try{
                service.wait();
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
                ConfigurationFixtures.createGoldenConfiguration(testingServer.getConnectString()));

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
}