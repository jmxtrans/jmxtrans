package com.googlecode.jmxtrans.cluster.zookeeper;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * ZookeeperClusterService. The zookeper based implementation of the ClusterService interface. Basically it is
 * anm individual thread that manages the connection to a zookeeper instance or cluster.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 * @see ClusterService
 */
public class ZookeeperClusterService extends Thread implements ClusterService, JvmConfigChangeListener {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperClusterService.class);
    private Configuration configuration;
    private Injector injector;

    private ZookeeperConfig clConfig;
    private CuratorFramework clClient;

    private PathChildrenCache jvmRootCache;
    private PathChildrenCache workerRootCache;

    private List<ClusterStateChangeListener> clusterStateChangeListeners = ImmutableList.of();
    private List<ConfigurationChangeListener> configurationChangeListeners = ImmutableList.of();

    Map<String, ZookeeperJvmHandler> jvmHandlers= new HashMap<>();

    Map<String, String> jmxTransConfigs = new HashMap<>();

    @Inject
    public ZookeeperClusterService(Injector injector, Configuration configuration) {
        this.injector = injector;
        this.configuration = configuration;
        log.info(this.getClass().getName() + " initiated");
    }

    /**
     * The main initialization method of the ClusterService. It is called from the constructor
     */
    private void initilaize() {
        clConfig = ZookeeperConfigBuilder.buildFromProperties(configuration);
        try {
            startClusterConnection();
            jvmRootCache    = new PathChildrenCache(clClient, clConfig.getConfigPath(), false);
            workerRootCache = new PathChildrenCache(clClient, clConfig.getHeartBeatPath(),false);
            jvmRootCache.getListenable().addListener(new JvmConfigPathChangeListener());
            workerRootCache.getListenable().addListener(new WorkerPathChangeListener());
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * Stat the CuratorFrameword client and add a listener that is listening for connection state changes.
     * @throws Exception
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
    public void startService() {
        initilaize();
        this.start();
    }

    @Override
    public void stopService() {
        this.interrupt();
    }

    @Override
    public void run() {
        try {
            startHeartBeating();
            queryConfigs();
            startChangeListeners();
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
        synchronized (this) {
            this.notifyAll();
        }

        while (!isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error(Throwables.getStackTraceAsString(e));
            }
        }

        try {
            stopHeartBeating();
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * Determine if the unerlaying ZookeeperClient is connected.
     * @return
     */
    public boolean isConnected(){
        return this.clClient.getZookeeperClient().isConnected();
    }

    /**
     * Create the heartbeating Ephemeral node on the Zookeeper. This node is for heartbeating and serveice discovery.
     * @throws Exception
     */
    private void startHeartBeating() throws Exception{
        this.clClient.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(clConfig.getWorkerNodePath());
    }

    /**
     * Remove the heartbeting node from the zookeeper.
     * @throws Exception
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
     * @throws Exception
     */
    private void startChangeListeners() throws Exception{
        if(isConnected()){
            jvmRootCache.start();
            workerRootCache.start();
        }
    }

    /**
     * Check if the heartbeat node is present on the zookeeper.
     * @return
     * @throws Exception
     */
    private Boolean isHeartBeating() throws Exception {
        Stat stat = this.clClient.checkExists().forPath(clConfig.getWorkerNodePath());
        return (null != stat);
    }

    /**
     * List the jvm nodes on the zookeeper and creaet a unique handler for each instance.
     * @throws Exception
     */
    private void queryConfigs() throws Exception {
        if (null == this.clClient.checkExists().forPath(this.clConfig.getConfigPath())) {
            throw new Exception("Znode is miissing! Have you started the cluster manager?");
        }
        List<String> jvms = this.clClient.getChildren().forPath(this.clConfig.getConfigPath());
        for (String i : jvms) {
            System.out.println("Jvm: " + i);
            ZookeeperJvmHandler handler = new ZookeeperJvmHandler(i, this.clClient, clConfig, this);

            this.jvmHandlers.put(i, handler);
        }
    }

    @Override
    public void jvmConfigChanged(String jvmAlias, String jvmConfig) {
        jmxTransConfigs.put(jvmAlias,jvmConfig);
        notifyConfigurationChangeListeners();
    }

    @Override
    public void jvmConfigRemoved(String jvmAlias) {
        jmxTransConfigs.remove(jvmAlias);
        notifyConfigurationChangeListeners();
    }

    private class JvmConfigPathChangeListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            String jvmAlias;
            switch (pathChildrenCacheEvent.getType()) {
                case CHILD_ADDED:
                    log.debug(ZookeeperClusterService.this.clConfig.getWorkerAlias()
                            + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
                    if (!ZookeeperClusterService.this.jvmHandlers.containsKey(jvmAlias)) {
                        ZookeeperJvmHandler handler = new ZookeeperJvmHandler(
                                jvmAlias, ZookeeperClusterService.this.clClient, clConfig, ZookeeperClusterService.this);
                        ZookeeperClusterService.this.jvmHandlers.put(jvmAlias, handler);
                    }
                    break;

                case CHILD_UPDATED:
                    log.debug(clConfig.getWorkerAlias() + " Node upadet: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_REMOVED:
                    jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
                    if (ZookeeperClusterService.this.jvmHandlers.containsKey(jvmAlias)) {
                        ZookeeperClusterService.this.jvmHandlers.get(jvmAlias).close();
                        ZookeeperClusterService.this.jvmHandlers.remove(jvmAlias);
                    }
                    log.debug(clConfig.getWorkerAlias() + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
            }
        }
    }

    private class WorkerPathChangeListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            switch (pathChildrenCacheEvent.getType()) {
                case CHILD_ADDED:
                    log.debug(clConfig.getWorkerAlias() + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_UPDATED:
                    log.debug(clConfig.getWorkerAlias() + " Node upadet: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_REMOVED:
                    for(ZookeeperJvmHandler i : ZookeeperClusterService.this.jvmHandlers.values()){
                        i.workerChanged();
                    }

                    log.debug(clConfig.getWorkerAlias() + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
            }
        }
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
     * Notify the registered listeners that the configaration changed.
     */
    private void notifyConfigurationChangeListeners(){
        String[] configs = (String[])jmxTransConfigs.values().toArray();

        Iterator<ConfigurationChangeListener> it = configurationChangeListeners.iterator();
        ConfigurationChangeEvent event = new ConfigurationChangeEvent(ConfigurationChangeEvent.Type.JVM_CONFIGURATION_CHANGED, configs);

        while (it.hasNext()){
            it.next().configurationChanged(event);
        }
    }

    /**
     * Notify the registered listeners about the state change on the cluster connction.
     * @param state
     */
    private void notifyClusterChangeListeners(ConnectionState state){
        Iterator<ClusterStateChangeListener> it = clusterStateChangeListeners.iterator();
        ClusterStateChangeEvent event = new ClusterStateChangeEvent(state);
        while (it.hasNext()){
            it.next().cluterStateChanged(event);
        }
    }
}
