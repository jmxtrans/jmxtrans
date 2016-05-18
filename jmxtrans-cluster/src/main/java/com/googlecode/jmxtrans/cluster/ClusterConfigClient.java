package com.googlecode.jmxtrans.cluster;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The main class that menages the connection to a cluster coordination
 * service. If the cluster connection is enabled, the configuration will
 * be queried from the cluster coordination service.
 */
public class ClusterConfigClient extends Thread implements ClusterConfigChangeListener{
    private Configuration clConfiguration;
    private String clServiceConnectionString;
    private CuratorFramework clClient;
    private String workerAlias = "";
    private String configPath = "";
    private String heartbeatPath = "";
    PathChildrenCache jvmRootCache;
    PathChildrenCache workerRootCache;
    private Map<String, ClusterJvmConfigHandler> jvmConfigs;

    private static final Logger log = LoggerFactory.getLogger(ClusterConfigClient.class);

    public ClusterConfigClient(){
        jvmConfigs = new HashMap<>();
    }

    public void initialize(File clConfigFile) throws ConfigurationException{
        this.clConfiguration = new PropertiesConfiguration(clConfigFile);
        initialize(clConfiguration.getString("zookeper.connectionstring"),
                clConfiguration.getString("zookeeper.workeralias"),
                clConfiguration.getString("zookeeper.heartbeatpath"),
                clConfiguration.getString("zookeeper.configpath"));
    }

    public void initialize(String clServiceConnectionString, String workerAlias, String heartbeatPath, String configPath) throws ConfigurationException{
        if(null == clServiceConnectionString || "".equals(clServiceConnectionString)){
            throw new ConfigurationException("None or empty connectionString!");
        }
        if(null == workerAlias || "".equals(workerAlias)){
            throw new ConfigurationException("The worker alias cannot be null or empty! Maybe 'cluster.alias' " +
                    "filed is missing from the configuration");
        }
        this.clServiceConnectionString = clServiceConnectionString;
        this.workerAlias = workerAlias;
        this.heartbeatPath = heartbeatPath;
        this.configPath = configPath;
        this.setName("Cluster-" + this.workerAlias);
    }


    @Override
    public void run() {
        try{
            startClusterConnection();
            startHeartBeating();
            queryConfigs();

            jvmRootCache = new PathChildrenCache(clClient, this.configPath, false);
            workerRootCache = new PathChildrenCache(clClient, this.heartbeatPath, false);

            jvmRootCache.getListenable().addListener(new JvmConfigPathChangeListener());
            workerRootCache.getListenable().addListener(new WorkerPathChangeListener());
            jvmRootCache.start();
            workerRootCache.start();

        } catch (Exception e) {
            e.printStackTrace();
        }


        while(!this.isInterrupted()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void interrupt() {
        try {
            stopHeartBeating();
            stopClusterConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            this.clClient.close();
        }
        super.interrupt();
    }

    private void startClusterConnection() throws Exception{
        this.clClient = CuratorFrameworkFactory
                .newClient(clServiceConnectionString, new ExponentialBackoffRetry(1000,3));
        this.clClient.start();
    }

    private void stopClusterConnection(){
        this.clClient.close();
    }

    private void startHeartBeating() throws Exception{
        Stat stat = this.clClient.checkExists().forPath(this.heartbeatPath);
        if(null == stat){
            this.clClient.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(this.heartbeatPath);
        }
        this.clClient.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(this.heartbeatPath + "/" + this.workerAlias);
    }

    private void stopHeartBeating() throws Exception{
        if(isHeartBeating()){
            this.clClient.delete()
                         .deletingChildrenIfNeeded()
                         .forPath(this.heartbeatPath + "/" + this.workerAlias);
        }
    }

    private Boolean isHeartBeating() throws Exception {
        Stat stat = this.clClient.checkExists().forPath(this.heartbeatPath + "/" + this.workerAlias);
        return (null != stat);
    }

    private void queryConfigs() throws Exception {
        if (null == this.clClient.checkExists().forPath(this.configPath)) {
            throw new Exception("Znode is miissing! Have you started the cluster manager?");
        }
        List<String> jvms = this.clClient.getChildren().forPath(this.configPath);
        for (String i : jvms) {
            ClusterJvmConfigHandler handler = new ClusterJvmConfigHandler(this.heartbeatPath, this.configPath, i, this.workerAlias, this.clClient, this);
            handler.initialize();

            this.jvmConfigs.put(i, handler);

        }
    }

    private class JvmConfigPathChangeListener implements PathChildrenCacheListener{

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            String jvmAlias;
            switch (pathChildrenCacheEvent.getType()) {
                case CHILD_ADDED:
                    log.debug(workerAlias + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
                    if (!ClusterConfigClient.this.jvmConfigs.containsKey(jvmAlias)) {
                        ClusterJvmConfigHandler handler = new ClusterJvmConfigHandler(
                                ClusterConfigClient.this.heartbeatPath, ClusterConfigClient.this.configPath,
                                jvmAlias, ClusterConfigClient.this.workerAlias, ClusterConfigClient.this.clClient,
                                ClusterConfigClient.this);
                        handler.initialize();
                        ClusterConfigClient.this.jvmConfigs.put(jvmAlias, handler);
                    }
                    break;

                case CHILD_UPDATED:
                    log.debug(workerAlias + " Node upadet: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_REMOVED:
                    jvmAlias = ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath());
                    if (ClusterConfigClient.this.jvmConfigs.containsKey(jvmAlias)) {
                        ClusterConfigClient.this.jvmConfigs.get(jvmAlias).dipose();
                        ClusterConfigClient.this.jvmConfigs.remove(jvmAlias);
                    }
                    log.debug(workerAlias + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
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
                    log.debug(workerAlias + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_UPDATED:
                    log.debug(workerAlias + " Node upadet: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;

                case CHILD_REMOVED:
                    Iterator it = ClusterConfigClient.this.jvmConfigs.entrySet().iterator();

                    for(ClusterJvmConfigHandler i : ClusterConfigClient.this.jvmConfigs.values()){
                        i.notifyWorkerpoolChange();
                    }

                    log.debug(workerAlias + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
            }
        }
    }
}
