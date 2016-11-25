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
import com.googlecode.jmxtrans.cluster.events.ClusterStateChangeEvent;
import com.googlecode.jmxtrans.cluster.events.ClusterStateChangeListener;
import com.googlecode.jmxtrans.cluster.events.ConfigurationChangeEvent;
import com.googlecode.jmxtrans.cluster.events.ConfigurationChangeListener;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ZookeeperClusterService. The zookeeper based implementation of the ClusterService interface. Basically it is
 * anm individual thread that manages the connection to a zookeeper instance or cluster.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 * @see ClusterService
 */
public class ZookeeperClusterService implements ClusterService, JvmConfigChangeListener {
	private static final Logger log = LoggerFactory.getLogger(ZookeeperClusterService.class);
	@Nonnull private final Configuration configuration;

	private ZookeeperConfig clConfig;
	private CuratorFramework clClient;

	private PathChildrenCache jvmRootCache;
	private PathChildrenCache workerRootCache;

	private List<ClusterStateChangeListener> clusterStateChangeListeners = new CopyOnWriteArrayList<>();
	private List<ConfigurationChangeListener> configurationChangeListeners = new CopyOnWriteArrayList<>();

	@Nonnull
	private final ConcurrentHashMap<String, ZookeeperJvmHandler> jvmHandlers= new ConcurrentHashMap<>();

	Map<String, String> jmxTransConfigs = new ConcurrentHashMap<>();

	@Inject
	public ZookeeperClusterService(@Nonnull Configuration configuration) {
		this.configuration = configuration;
		log.debug(this.getClass().getName() + " initiated");
	}

	/**
	 * The main initialization method of the ClusterService.
	 */
	private void initialize() {
		// TODO: externalize construction
		clConfig = ZookeeperConfigBuilder.buildFromProperties(configuration);
		try {
			startClusterConnection();
			jvmRootCache    = new PathChildrenCache(clClient, clConfig.getConfigPath(), false);
			workerRootCache = new PathChildrenCache(clClient, clConfig.getHeartBeatPath(),false);
			jvmRootCache.getListenable().addListener(new JvmConfigPathChangeListener(clConfig, jvmHandlers, clClient, this));
			workerRootCache.getListenable().addListener(new WorkerPathChangeListener(clConfig, jvmHandlers));
		} catch (Exception e) {
			log.error("Error initializing ZookeeperClusterService", e);
		}
	}

	/**
	 * Stat the CuratorFramework client and add a listener that is listening for connection state changes.
	 */
	private void startClusterConnection() throws Exception {
		this.clClient = CuratorFrameworkFactory
				.newClient(clConfig.getConnectionString(),
						new ExponentialBackoffRetry(clConfig.getConnectTimeout(), clConfig.getConnectRetry()));
		clClient.start();
		clClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			@Override
			public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
				notifyClusterChangeListeners(connectionState);
			}
		});
	}

	@Override
	public void startService() throws Exception {
		initialize();
		startHeartBeating();
		try {
			queryConfigs();
		} catch (IllegalStateException e) {
			// TODO: make sure starting service does not throw exception in the nominal case and let exception
			// bubble up if they happen.
			// FIXME: queryConfigs() require the config path to be already created. This is not ensured at the moment.
			log.error("Error while querying configs.", e);
		}
		startChangeListeners();
	}

	@Override
	public void stopService() throws Exception {
		stopHeartBeating();
	}

	/**
	 * Determine if the underlying ZookeeperClient is connected.
	 */
	public boolean isConnected(){
		return this.clClient.getZookeeperClient().isConnected();
	}

	/**
	 * Create the heart beating Ephemeral node on the Zookeeper. This node is for heart beating and service discovery.
	 */
	private void startHeartBeating() throws Exception{
		this.clClient.create()
				.creatingParentContainersIfNeeded()
				.withMode(CreateMode.EPHEMERAL)
				.forPath(clConfig.getWorkerNodePath());
	}

	/**
	 * Remove the heart beating node from the zookeeper.
	 */
	private void stopHeartBeating() throws Exception{
		if(isConnected() && isHeartBeating()){
			this.clClient.delete()
					.deletingChildrenIfNeeded()
					.forPath(clConfig.getWorkerNodePath());
		}
	}

	/**
	 * Start the path caches that detect if a worker or jvm added, removed or changed.
	 */
	private void startChangeListeners() throws Exception{
		if(isConnected()){
			jvmRootCache.start();
			workerRootCache.start();
		}
	}

	/**
	 * Check if the heartbeat node is present on the zookeeper.
	 */
	private boolean isHeartBeating() throws Exception {
		Stat stat = this.clClient.checkExists().forPath(clConfig.getWorkerNodePath());
		return (null != stat);
	}

	/**
	 * List the jvm nodes on the zookeeper and create a unique handler for each instance.
	 */
	private void queryConfigs() throws Exception {
		if (null == this.clClient.checkExists().forPath(this.clConfig.getConfigPath())) {
			throw new IllegalStateException("Znode is missing! Have you started the cluster manager?");
		}
		List<String> jvms = this.clClient.getChildren().forPath(clConfig.getConfigPath());
		for (String i : jvms) {
			System.out.println("Jvm: " + i);
			ZookeeperJvmHandler handler = new ZookeeperJvmHandler(i, clClient, clConfig, this);

			this.jvmHandlers.put(i, handler);
		}
	}

	@Override
	public void jvmConfigChanged(String jvmAlias, String jvmConfig) {
		jmxTransConfigs.put(jvmAlias, jvmConfig);
		notifyConfigurationChangeListeners();
	}

	@Override
	public void jvmConfigRemoved(String jvmAlias) {
		jmxTransConfigs.remove(jvmAlias);
		notifyConfigurationChangeListeners();
	}

	@Override
	public void registerStateChangeListener(ClusterStateChangeListener stateChangeListener){
		this.clusterStateChangeListeners.add(stateChangeListener);
	}

	@Override
	public void unregisterStateChangeListener(ClusterStateChangeListener stateChangeListener){
		this.clusterStateChangeListeners.remove(stateChangeListener);
	}

	@Override
	public void registerConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener){
		this.configurationChangeListeners.add(configurationChangeListener);
	}

	@Override
	public void unregisterConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener){
		this.configurationChangeListeners.remove(configurationChangeListener);
	}

	/**
	 * Notify the registered listeners that the configuration changed.
	 */
	private void notifyConfigurationChangeListeners(){
		ConfigurationChangeEvent event = new ConfigurationChangeEvent(ConfigurationChangeEvent.Type.JVM_CONFIGURATION_CHANGED, jmxTransConfigs.values());

		for (ConfigurationChangeListener listener : configurationChangeListeners) {
			listener.configurationChanged(event);
		}
	}

	/**
	 * Notify the registered listeners about the state change on the cluster connection.
	 */
	private void notifyClusterChangeListeners(ConnectionState state){
		ClusterStateChangeEvent event = new ClusterStateChangeEvent(state);

		for (ClusterStateChangeListener listener : clusterStateChangeListeners) {
			listener.cluterStateChanged(event);
		}
	}

	private static class JvmConfigPathChangeListener implements PathChildrenCacheListener {

		@Nonnull private final ZookeeperConfig clConfig;
		@Nonnull private final ConcurrentHashMap<String, ZookeeperJvmHandler> jvmHandlers;
		@Nonnull private final CuratorFramework clClient;
		@Nonnull private final ZookeeperClusterService zookeeperClusterService;

		public JvmConfigPathChangeListener(
				@Nonnull ZookeeperConfig clConfig,
				@Nonnull ConcurrentHashMap<String, ZookeeperJvmHandler> jvmHandlers,
				@Nonnull CuratorFramework clClient,
				@Nonnull ZookeeperClusterService zookeeperClusterService) {
			this.clConfig = clConfig;
			this.jvmHandlers = jvmHandlers;
			this.clClient = clClient;
			this.zookeeperClusterService = zookeeperClusterService;
		}

		@Override
		public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
			String jvmAlias;
			switch (pathChildrenCacheEvent.getType()) {
				case CHILD_ADDED:
					log.debug("{} Node added: {}", clConfig.getWorkerAlias(),
							ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
					jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
					if (!this.jvmHandlers.containsKey(jvmAlias)) {
						ZookeeperJvmHandler handler = new ZookeeperJvmHandler(
								jvmAlias, clClient, clConfig, zookeeperClusterService);
						jvmHandlers.put(jvmAlias, handler);
					}
					break;

				case CHILD_UPDATED:
					log.debug(clConfig.getWorkerAlias() + " Node update: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
					break;

				case CHILD_REMOVED:
					jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
					ZookeeperJvmHandler jvmHandler = jvmHandlers.remove(jvmAlias);
					if (jvmHandler != null) jvmHandler.close();
					log.debug("{} Node removed: {}", clConfig.getWorkerAlias(),
							ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
					break;

				default:
					throw new IllegalArgumentException("Unknown event type: " + pathChildrenCacheEvent.getType());
			}
		}
	}

	private static class WorkerPathChangeListener implements PathChildrenCacheListener {
		@Nonnull private final Map<String, ZookeeperJvmHandler> jvmHandlers;
		@Nonnull private final ZookeeperConfig clConfig;

		public WorkerPathChangeListener(
				@Nonnull ZookeeperConfig clConfig, @Nonnull Map<String, ZookeeperJvmHandler> jvmHandlers) {
			this.jvmHandlers = jvmHandlers;
			this.clConfig = clConfig;
		}

		@Override
		public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
			switch (pathChildrenCacheEvent.getType()) {
				case CHILD_ADDED:
					log.debug(clConfig.getWorkerAlias() + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
					break;

				case CHILD_UPDATED:
					log.debug(clConfig.getWorkerAlias() + " Node updated: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
					break;

				case CHILD_REMOVED:
					for(ZookeeperJvmHandler i : jvmHandlers.values()){
						i.workerChanged();
					}

					log.debug(clConfig.getWorkerAlias() + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
					break;

				default:
					throw new IllegalArgumentException("Unknown event type: " + pathChildrenCacheEvent.getType());
			}
		}
	}
}
