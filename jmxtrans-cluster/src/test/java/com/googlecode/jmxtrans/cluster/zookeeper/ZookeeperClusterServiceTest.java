/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.cluster.zookeeper;

import com.googlecode.jmxtrans.cluster.ClusterService;
import com.googlecode.jmxtrans.cluster.ClusterServiceFactory;
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

import static org.assertj.core.api.Assertions.assertThat;

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
public class ZookeeperClusterServiceTest {

	private static TestingServer testingServer;
	private static CuratorFramework client;

	@BeforeClass
	public static void beforeClass() throws Exception {
		testingServer = new TestingServer(2181, true);
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
				ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString()));
		service.startService();

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
				ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString()));

		service.startService();

		client.setData().forPath("/jmxtrans/jvms/jvm_01/config", "Config01 Update".getBytes());
		service.stopService();
	}
}