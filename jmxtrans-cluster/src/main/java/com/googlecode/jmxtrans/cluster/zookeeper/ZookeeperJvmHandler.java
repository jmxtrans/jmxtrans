package com.googlecode.jmxtrans.cluster.zookeeper;

import com.google.common.base.Throwables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by kulcsart on 5/18/2016.
 *
 */
public class ZookeeperJvmHandler {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperJvmHandler.class);

    private final String jvmAlias;
    private final CuratorFramework clClient;
    private final ZookeeperConfig clConfig;

    private byte[] jmxTransConfig;

    private String affinity;
    private boolean isRequested = false;

    private PathChildrenCache configCache;
    private TreeCache ownershipCache;

    private NodeCache affinityWorkerNodeCache;

    private InterProcessSemaphoreMutex ownerLock;

    public ZookeeperJvmHandler(String jvmAlias, CuratorFramework clClient, ZookeeperConfig clConfig){
        checkArgument(jvmAlias.length() > 0, "Jvm alias name cannot be null or empty!");
        this.jvmAlias = jvmAlias;
        this.clClient = checkNotNull(clClient, "Zookeeper client cannot be null!");
        this.clConfig = checkNotNull(clConfig, "Configuration cannot be null!");
    }

    public void initialize() throws Exception{
        try {
            ownerLock = new InterProcessSemaphoreMutex(clClient, clConfig.getOwnerNodePath(this.jvmAlias));
            configCache = new PathChildrenCache(clClient, clConfig.getJvmPath(this.jvmAlias), false);
            ownershipCache = new TreeCache(clClient, clConfig.getOwnerNodePath(this.jvmAlias));

            checkNotNull(this.clClient.checkExists().forPath(clConfig.getOwnerNodePath(this.jvmAlias)),
                    "The JVM - %s clConfig does not exists in Zookeeper!", this.jvmAlias);
            checkNotNull(this.clClient.checkExists().forPath(clConfig.getJvmAffinityNodePath(this.jvmAlias)),
                    "The affinity of Jvm %s cannot be determinded!", this.jvmAlias);

            readAffinityFromZookeeper();

            configCache.getListenable().addListener(new ConfigPathChangeListener());
            configCache.start();

            ownershipCache.getListenable().addListener(new OwnershipTreeChangeListener());
            ownershipCache.start();

            updateStateMachine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispose() throws IOException{
        configCache.close();
        ownershipCache.close();
    }


    private Boolean hasAffinityForJvm(){
        return this.clConfig.getWorkerAlias().equals(this.affinity);
    }

    private void setUpOwnershipRequest() {
        try{
            Stat stat = this.clClient.checkExists().forPath(this.clConfig.getRequestNodePath(this.jvmAlias));
            if(null == stat) {
                this.clClient.create().withMode(CreateMode.EPHEMERAL).forPath(this.clConfig.getRequestNodePath(this.jvmAlias),
                        this.clConfig.getWorkerAlias().getBytes());
            }
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }


    private void takeOwnership(Boolean requeastIfFails) throws Exception{
        if(ownerLock.isAcquiredInThisProcess()){
            readConfigFromZookeeper();
            return;
        }

        if(ownerLock.acquire(2, TimeUnit.SECONDS)){
            readConfigFromZookeeper();
            return;
        } else if(requeastIfFails){
                setUpOwnershipRequest();
        }
        log.debug( "Get the ownewrship for " + this.jvmAlias + " owner:" + ownerLock.isAcquiredInThisProcess());
    }

    private void releaseOwnership(){
        try {
            log.debug("Release " + this.clConfig.getWorkerAlias()+ " " + this.jvmAlias + " owner:" + ownerLock.isAcquiredInThisProcess());
            if(ownerLock.isAcquiredInThisProcess()) {
                ownerLock.release();
            }
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    private void updateStateMachine() throws Exception{
        if(ownerLock.isAcquiredInThisProcess()){
            log.debug(clConfig.getWorkerAlias() + " has the ownership for " + this.jvmAlias);
            return;
        }
        if(hasAffinityForJvm()){
            log.debug("Worker " + clConfig.getWorkerAlias() + " tries to get the ownership to AFFINITY jvm " + this.jvmAlias);
            takeOwnership(true);
            return;
        }

        try {
            if(null == this.clClient.checkExists().forPath(clConfig.getHeartBeatPath() + "/" + this.affinity)){
                log.debug("Worker " + this.clConfig.getWorkerAlias() + " tries to get the ownership to REMOTE jvm " + jvmAlias);
                takeOwnership(false);
            }
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    public void notifyWorkerChange() throws Exception {
        updateStateMachine();
    }

    private void readAffinityFromZookeeper() throws Exception{
        String newAffinity = new String(ZookeeperJvmHandler.this.clClient.getData().forPath(
                clConfig.getJvmAffinityNodePath(ZookeeperJvmHandler.this.jvmAlias)));

        if(newAffinity.length() > 0 && !newAffinity.equals(this.affinity)){
            this.affinity = newAffinity;
        }
    }

    private void readConfigFromZookeeper() throws Exception{
        if(this.ownerLock.isAcquiredInThisProcess()){
            this.jmxTransConfig = this.clClient.getData().forPath(clConfig.getConfigNodePath(this.jvmAlias));
        }
    }

    private class ConfigPathChangeListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            switch ( pathChildrenCacheEvent.getType() ) {
                case CHILD_ADDED:
                case CHILD_UPDATED:
                    switch (ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath())){
                        case ZookeeperConfig.AFFINITY_NODE_NAME:
                            readAffinityFromZookeeper();
                            break;
                        case ZookeeperConfig.CONFIG_NODE_NAME:
                            readConfigFromZookeeper();
                            break;
                        case ZookeeperConfig.REQUEST_NODE_NAME:
                            if(ownerLock.isAcquiredInThisProcess() && !hasAffinityForJvm()){
                                releaseOwnership();
                            }
                    }
                    log.debug(clConfig.getWorkerAlias() + " Node changed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()) +
                            new String(clClient.getData().forPath(pathChildrenCacheEvent.getData().getPath())));
                    break;

                case CHILD_REMOVED:
                    switch (ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath())){
                        case ZookeeperConfig.AFFINITY_NODE_NAME:
                            affinity = null;
                            break;
                        case ZookeeperConfig.CONFIG_NODE_NAME:
                            jmxTransConfig = null;
                            break;
                        case ZookeeperConfig.REQUEST_NODE_NAME:
                            isRequested = false;
                            break;
                    }
                    log.debug(clConfig.getWorkerAlias() + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
            }
        }
    }

    private class OwnershipTreeChangeListener implements TreeCacheListener {

        @Override
        public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
            switch (treeCacheEvent.getType()){
                case NODE_ADDED:
                    log.debug(clConfig.getWorkerAlias() + ": Tree node added: " + ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath()));
                    break;
                case NODE_UPDATED:
                    log.debug(clConfig.getWorkerAlias() + ": Tree node updated: " + ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath()));
                    break;
                case NODE_REMOVED:
                    log.debug(clConfig.getWorkerAlias() + ": Tree node removed: " + ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath()));
                    break;
            }
            updateStateMachine();
        }
    }
}
