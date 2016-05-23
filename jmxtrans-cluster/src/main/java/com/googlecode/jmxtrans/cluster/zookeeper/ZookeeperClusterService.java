package com.googlecode.jmxtrans.cluster.zookeeper;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.cluster.ClusterService;
import com.googlecode.jmxtrans.cluster.events.ClusterStateChangeListener;
import com.googlecode.jmxtrans.cluster.events.ConfigurationChangeListener;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ZookeeperClusterService. The zookeper based implementation of the ClusterService interface. Basically it is
 * anm individual thread that manages the connection to a zookeeper instance or cluster.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 * @see ClusterService
 */
public class ZookeeperClusterService extends Thread implements ClusterService {
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

    @Inject
    public ZookeeperClusterService(Injector injector, Configuration configuration) {
        this.injector = injector;
        this.configuration = configuration;
        log.info(this.getClass().getName() + " initiated");
    }

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

    private void startClusterConnection() throws Exception {
        this.clClient = CuratorFrameworkFactory
                .newClient(clConfig.getConnectionString(),
                        new ExponentialBackoffRetry(clConfig.getConnectTimeout(), clConfig.getConnectRetry()));
        clClient.start();
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

    public boolean isConnected(){
        return this.clClient.getZookeeperClient().isConnected();
    }

    private void startHeartBeating() throws Exception{
        this.clClient.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(clConfig.getWorkerNodePath());
    }

    private void stopHeartBeating() throws Exception{
        if(isConnected() && isHeartBeating()){
            this.clClient.delete()
                    .deletingChildrenIfNeeded()
                    .forPath(clConfig.getWorkerNodePath());
        }
    }

    private void startChangeListeners() throws Exception{
        if(isConnected()){
            jvmRootCache.start();
            workerRootCache.start();
        }
    }

    private Boolean isHeartBeating() throws Exception {
        Stat stat = this.clClient.checkExists().forPath(clConfig.getWorkerNodePath());
        return (null != stat);
    }

    private void queryConfigs() throws Exception {
        if (null == this.clClient.checkExists().forPath(this.clConfig.getConfigPath())) {
            throw new Exception("Znode is miissing! Have you started the cluster manager?");
        }
        List<String> jvms = this.clClient.getChildren().forPath(this.clConfig.getConfigPath());
        for (String i : jvms) {
            System.out.println("Jvm: " + i);
            ZookeeperJvmHandler handler = new ZookeeperJvmHandler(i, this.clClient, clConfig);
            handler.initialize();

            this.jvmHandlers.put(i, handler);

        }
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
                                jvmAlias, ZookeeperClusterService.this.clClient, clConfig);
                        handler.initialize();
                        ZookeeperClusterService.this.jvmHandlers.put(jvmAlias, handler);
                    }
                    break;

                case CHILD_UPDATED:
                    log.debug(clConfig.getWorkerAlias() + " Node upadet: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_REMOVED:
                    jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
                    if (ZookeeperClusterService.this.jvmHandlers.containsKey(jvmAlias)) {
                        ZookeeperClusterService.this.jvmHandlers.get(jvmAlias).dispose();
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
            String jvmAlias;
            switch (pathChildrenCacheEvent.getType()) {
                case CHILD_ADDED:
                    log.debug(clConfig.getWorkerAlias() + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_UPDATED:
                    log.debug(clConfig.getWorkerAlias() + " Node upadet: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_REMOVED:
                    Iterator it = ZookeeperClusterService.this.jvmHandlers.entrySet().iterator();

                    for(ZookeeperJvmHandler i : ZookeeperClusterService.this.jvmHandlers.values()){
                        i.notifyWorkerChange();
                    }

                    log.debug(clConfig.getWorkerAlias() + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
            }
        }
    }

    public void registerStateChangeListener(ClusterStateChangeListener stateChangeListener){
        this.clusterStateChangeListeners.add(stateChangeListener);
    }
    public void unregisterStateChangeListener(ClusterStateChangeListener stateChangeListener){
        this.clusterStateChangeListeners.remove(stateChangeListener);
    }
    public void registerConfigurationChangeListeners(ConfigurationChangeListener configurationChangeListener){
        this.configurationChangeListeners.add(configurationChangeListener);
    }
    public void unregisterConfigurationChangeListeners(ConfigurationChangeListener configurationChangeListener){
        this.configurationChangeListeners.remove(configurationChangeListener);
    }
}
