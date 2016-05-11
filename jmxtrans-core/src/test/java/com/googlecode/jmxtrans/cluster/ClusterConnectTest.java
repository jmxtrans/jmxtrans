package com.googlecode.jmxtrans.cluster;

import com.kaching.platform.testing.AllowDNSResolution;
import com.kaching.platform.testing.AllowNetworkAccess;
import com.kaching.platform.testing.AllowNetworkListen;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * ClusterConfigClient Tester.
 *
 * @author Tibor Kulcsar
 * @version 1.0
 * @since <pre>May 5, 2016</pre>
 */
@AllowDNSResolution
@AllowNetworkListen(ports = {0})
@AllowNetworkAccess(endpoints = {"127.0.0.1:*"})
public class ClusterConnectTest {

    CuratorFramework clClient;
    Configuration clConfig;

    private static final Logger log = LoggerFactory.getLogger(ClusterConnectTest.class);

    @Before
    public void before() throws Exception {
        if(null == clClient || !clClient.getState().equals((CuratorFrameworkState.STARTED))){
            setupClusterClient();
        }
    }

    @After
    public void after() throws Exception {
    }

    @Ignore @Test
    public void testInterProcessLock() throws Exception{
        InterProcessMutex lock  = new InterProcessMutex(clClient, "/testcases/iplock" );
        InterProcessMutex lock2  = new InterProcessMutex(clClient, "/testcases/iplock" );
        lock.acquire(10, TimeUnit.SECONDS);

        Thread.sleep(2000);
        lock2.acquire(2,TimeUnit.SECONDS);
        Thread.sleep(2000);

        log.info("Lock 1 has to lock: " + lock.isAcquiredInThisProcess());
        log.info("Lock 2 has to lock: " + lock2.isAcquiredInThisProcess());

        Thread.sleep(120000);

        log.info("Lock 1 has to lock: " + lock.isAcquiredInThisProcess());
        log.info("Lock 2 has to lock: " + lock2.isAcquiredInThisProcess());
    }

    @Test
    public void testZookeeperConnection() throws Exception {
        try {
            clClient.create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/jmxtrans/jvms/jvm_01/config", "demo config 1".getBytes());
            clClient.create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/jmxtrans/jvms/jvm_01/affinity", ("worker_01").getBytes());

            clClient.create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/jmxtrans/jvms/jvm_02/config", "demo config 2".getBytes());
            clClient.create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/jmxtrans/jvms/jvm_02/affinity", ("worker_02").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClusterConfigClient clusterConnect1 = new ClusterConfigClient();
        clusterConnect1.initialize(file("cluster.properties"));

        ClusterConfigClient clusterConnect2 = new ClusterConfigClient();
        clusterConnect1.initialize(clConfig.getString("cluster.connectionstring"),"worker_02");



        clusterConnect1.start();
        Thread.sleep(5000);
        clusterConnect2.start();
        Thread.sleep(5000);
        clClient.setData().forPath("/jmxtrans/jvms/jvm_01/config", "demo config 1 updated".getBytes());
        Thread.sleep(120000);
        /*Thread.sleep(3000);
        Stat stat = clClient.checkExists().forPath("/jmxtrans/workers/" + clConfig.getString("cluster.alias"));
        assertFalse(null == stat);
        Thread.sleep(60000);
        clusterConnect.interrupt();
        stat = clClient.checkExists().forPath("/jmxtrans/workers/" + clConfig.getString("cluster.alias"));
        assertTrue(null == stat);
*/

        clClient.delete().deletingChildrenIfNeeded().forPath("/jmxtrans/jvms/jvm_01");
        clClient.delete().deletingChildrenIfNeeded().forPath("/jmxtrans/jvms/jvm_02");

    }


    /**
     * Method: initialize(File clConfigFile)
     */
    @Ignore
    @Test
    public void testInitializeClConfigFile() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: initialize(String clServiceConnectionString, String workerAlias)
     */
    @Ignore
    @Test
    public void testInitializeForClServiceConnectionStringWorkerAlias() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: run()
     */
    @Ignore
    @Test
    public void testRun() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: interrupt()
     */
    @Ignore
    @Test
    public void testInterrupt() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: uncaughtException(Thread var1, Throwable var2)
     */
    @Ignore
    @Test
    public void testUncaughtException() throws Exception {
//TODO: Test goes here... 
    }

    private File file(String filename) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(filename).toURI());
    }

    private CuratorFramework setupClusterClient() {
        try {
            clConfig = new PropertiesConfiguration(file("cluster.properties"));
            clClient = CuratorFrameworkFactory.newClient(clConfig.getString("cluster.connectionstring"), new ExponentialBackoffRetry(1000, 3));
            clClient.start();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return clClient;
    }


} 
