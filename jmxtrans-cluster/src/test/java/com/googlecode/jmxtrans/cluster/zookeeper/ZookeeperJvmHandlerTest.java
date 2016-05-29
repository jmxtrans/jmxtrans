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

import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.kaching.platform.testing.AllowDNSResolution;
import com.kaching.platform.testing.AllowLocalFileAccess;
import com.kaching.platform.testing.AllowNetworkAccess;
import com.kaching.platform.testing.AllowNetworkListen;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ZookeeperJvmHandler Tester.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 25, 2016</pre>
 */
@AllowDNSResolution
@AllowNetworkListen(ports = {0})
@AllowNetworkAccess(endpoints = {"127.0.0.1:*"})
@AllowLocalFileAccess(paths = {"*"})
@Category({IntegrationTest.class, RequiresIO.class})
public class ZookeeperJvmHandlerTest {

	private static TestingServer testingServer;
	private static CuratorFramework client;

	private JvmConfigChangeListener spyListener1;
	private JvmConfigChangeListener spyListener2;

	@BeforeClass
	public static void beforeClass() throws Exception{
		testingServer = new TestingServer(2181,true);
		client = CuratorFrameworkFactory.newClient(testingServer.getConnectString(), new ExponentialBackoffRetry(1000,3));
		client.start();
	}

	@Before
	public void before() throws Exception{
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath("/jmxtrans/jvms/jvm_01/config", "{jvm_01}".getBytes());
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath("/jmxtrans/jvms/jvm_01/affinity", "worker_01".getBytes());
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath("/jmxtrans/jvms/jvm_01/owner");

		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath("/jmxtrans/jvms/jvm_02/config", "{jvm_02}".getBytes());
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath("/jmxtrans/jvms/jvm_02/affinity", "worker_02".getBytes());
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.PERSISTENT)
				.forPath("/jmxtrans/jvms/jvm_02/owner");
	}

	@After
	public void after() throws Exception{
		client.delete().deletingChildrenIfNeeded().forPath("/jmxtrans");
	}


	@Test
	public void testAffinityBasedOwnership() throws Exception{
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_01");
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_02");


		Configuration config1 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config1.setProperty("zookeeper.workeralias", "worker_01");
		Configuration config2 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config2.setProperty("zookeeper.workeralias", "worker_02");

		spyListener1 = Mockito.spy(JvmConfigChangeListener.class);
		spyListener2 = Mockito.spy(JvmConfigChangeListener.class);

		ZookeeperJvmHandler handler01 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config1),
				spyListener1);

		ZookeeperJvmHandler handler02 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config2),
				spyListener2);

		Thread.sleep(1000);

		assertThat(handler01.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.AFFINITY_WORKER);
		assertThat(handler02.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.AFFINITY_WORKER);

		assertThat(handler01.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.OWNER);
		assertThat(handler02.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.NOT_OWNER);

		verify(spyListener1, atLeastOnce()).jvmConfigChanged("jvm_01", "{jvm_01}");
		verify(spyListener2, never()).jvmConfigChanged("jvm_01", "{jvm_01}");

	}

	@Test
	public void testLeadeLatchBasedOwnership() throws Exception{
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_03");
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_04");


		Configuration config1 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config1.setProperty("zookeeper.workeralias", "worker_03");
		Configuration config2 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config2.setProperty("zookeeper.workeralias", "worker_04");

		spyListener1 = Mockito.spy(JvmConfigChangeListener.class);

		ZookeeperJvmHandler handler01 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config1),
				spyListener1);

		Thread.sleep(1000);

		ZookeeperJvmHandler handler02 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config2),
				spyListener1);

		Thread.sleep(1000);

		assertThat(handler01.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.LEADER_ELECTION);
		assertThat(handler02.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.LEADER_ELECTION);

		assertThat( handler01.getOwnerState().equals(ZookeeperJvmHandler.OwnerState.OWNER) ^
					handler02.getOwnerState().equals(ZookeeperJvmHandler.OwnerState.OWNER)).isEqualTo(true);

		verify(spyListener1, times(1)).jvmConfigChanged("jvm_01", "{jvm_01}");
	}

	@Test
	public void testSwitchToAffinityFromLeaderElection() throws Exception{
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_03");
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_04");


		Configuration config1 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config1.setProperty("zookeeper.workeralias", "worker_03");
		Configuration config2 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config2.setProperty("zookeeper.workeralias", "worker_04");

		spyListener1 = Mockito.spy(JvmConfigChangeListener.class);
		spyListener2 = Mockito.spy(JvmConfigChangeListener.class);

		ZookeeperJvmHandler handler01 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config1),
				spyListener1);

		ZookeeperJvmHandler handler02 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config2),
				spyListener1);

		Thread.sleep(1000);


		Configuration config3 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config3.setProperty("zookeeper.workeralias", "worker_01");

		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_01");

		ZookeeperJvmHandler handler03 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config3),
				spyListener2);

		handler01.workerChanged();
		handler02.workerChanged();

		Thread.sleep(1000);


		assertThat(handler01.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.AFFINITY_WORKER);
		assertThat(handler02.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.AFFINITY_WORKER);
		assertThat(handler03.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.AFFINITY_WORKER);

		assertThat(handler01.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.NOT_OWNER);
		assertThat(handler02.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.NOT_OWNER);
		assertThat(handler03.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.OWNER);

		verify(spyListener1, atLeastOnce()).jvmConfigChanged("jvm_01", "{jvm_01}");
		verify(spyListener1, atLeastOnce()).jvmConfigRemoved("jvm_01");
		verify(spyListener2, times(1)).jvmConfigChanged("jvm_01", "{jvm_01}");
	}

	@Test
	public void testSwitchToLeaderLatchFromAffinity() throws Exception{
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_01");
		client.create().creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath("/jmxtrans/workers/worker_02");


		Configuration config1 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config1.setProperty("zookeeper.workeralias", "worker_01");
		Configuration config2 = ConfigurationFixtures.goldenConfiguration(testingServer.getConnectString());
		config2.setProperty("zookeeper.workeralias", "worker_02");

		spyListener1 = Mockito.spy(JvmConfigChangeListener.class);
		spyListener2 = Mockito.spy(JvmConfigChangeListener.class);

		ZookeeperJvmHandler handler01 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config1),
				spyListener1);

		ZookeeperJvmHandler handler02 = new ZookeeperJvmHandler("jvm_01",
				client,
				ZookeeperConfigBuilder.buildFromProperties(config2),
				spyListener2);

		Thread.sleep(1000);


		client.delete().deletingChildrenIfNeeded()
				.forPath("/jmxtrans/workers/worker_01");

		handler01.close();

		handler02.workerChanged();

		Thread.sleep(1000);


		assertThat(handler01.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.LATENT);
		assertThat(handler02.getOwnerMode()).isEqualTo(ZookeeperJvmHandler.OwnerMode.LEADER_ELECTION);

		assertThat(handler01.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.NOT_OWNER);
		assertThat(handler02.getOwnerState()).isEqualTo(ZookeeperJvmHandler.OwnerState.OWNER);

		verify(spyListener1, times(1)).jvmConfigChanged("jvm_01", "{jvm_01}");
		verify(spyListener1, times(1)).jvmConfigRemoved("jvm_01");

		verify(spyListener2, times(1)).jvmConfigChanged("jvm_01", "{jvm_01}");
	}

	@AfterClass
	public static void afterClass() throws Exception{
		client.close();
		testingServer.stop();
	}
}
