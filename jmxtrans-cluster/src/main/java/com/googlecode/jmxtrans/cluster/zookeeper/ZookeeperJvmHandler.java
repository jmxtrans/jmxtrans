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

    private String jmxTransConfig;

    private String affinity;

    PathChildrenCache configCache;
    TreeCache ownershipCache;
    InterProcessSemaphoreMutex ownerLock;

    public ZookeeperJvmHandler(String jvmAlias, CuratorFramework clClient, ZookeeperConfig clConfig){
        checkArgument(jvmAlias.length() > 0, "Jvm alias name cannot be null or empty!");
        checkNotNull(clClient, "Zookeeper client cannot be nuul!");
        checkNotNull(clConfig, "Configuration cannot be null!");
        this.jvmAlias = jvmAlias;
        this.clClient = clClient;
        this.clConfig = clConfig;
    }

    public void initialize() throws Exception{
        try {
            ownerLock = new InterProcessSemaphoreMutex(clClient, clConfig.getOwnerNodePath(this.jvmAlias));
            configCache = new PathChildrenCache(clClient, clConfig.getJvmPath(this.jvmAlias), false);
            ownershipCache = new TreeCache(clClient, clConfig.getOwnerNodePath(this.jvmAlias));

            checkNotNull(this.clClient.checkExists().forPath(clConfig.getOwnerNodePath(this.jvmAlias)),
                    "The JVM - %s clConfig does not exists in Zookeeper!", this.jvmAlias);

            checkNotNull(this.clClient.checkExists().forPath(clConfig.getAffinityNodePath(this.jvmAlias)),
                    "The affinity of Jvm %s cannot be determinded!", this.jvmAlias);

            getRequiredFields();
            runLeaderElection();

            configCache.getListenable().addListener(new ConfigPathChangeListener());
            configCache.start();

            ownershipCache.getListenable().addListener(new OwnershipTreeChangeListener());
            ownershipCache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispose(){
        try {
            configCache.close();
            ownershipCache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getRequiredFields() throws Exception{
        this.affinity = new String(this.clClient.getData().forPath(clConfig.getAffinityNodePath(this.jvmAlias)));
    }

    private void readConfigFromZookeeper() throws Exception{
        if(this.ownerLock.isAcquiredInThisProcess()){
            this.jmxTransConfig = new String(this.clClient.getData().forPath(clConfig.getConfigNodePath(this.jvmAlias)));
        }
        else
        {
            this.jmxTransConfig = null;
        }
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


    private void takeOwnership(Boolean requeastIfFails){
        try{
            if(!ownerLock.acquire(2, TimeUnit.SECONDS)){
                setUpOwnershipRequest();
            }

        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
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

    private void runLeaderElection(){
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

    public void notifyWorkerChange(){
        runLeaderElection();
    }

    private class ConfigPathChangeListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            getRequiredFields();

            switch ( pathChildrenCacheEvent.getType() )
            {
                case CHILD_ADDED:
                    log.debug(clConfig.getWorkerAlias() + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    if(ZookeeperConfig.REQUEST_NODE_NAME.equals(ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()))
                            && !hasAffinityForJvm()
                            && ownerLock.isAcquiredInThisProcess())
                    {
                        log.debug("Affinity worker arrived for " + jvmAlias);
                        releaseOwnership();
                    }
                    break;

                case CHILD_UPDATED:
                {
                    log.debug(clConfig.getWorkerAlias() + " Node changed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()) +
                            new String(clClient.getData().forPath(pathChildrenCacheEvent.getData().getPath())));


                    if(ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()).equals(ZookeeperConfig.AFFINITY_NODE_NAME)){
                        ZookeeperJvmHandler.this.getRequiredFields();
                        break;
                    }

                    if(ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()).equals(ZookeeperConfig.CONFIG_NODE_NAME)){
                        ZookeeperJvmHandler.this.readConfigFromZookeeper();
                        break;
                    }
                    break;
                }

                case CHILD_REMOVED:
                {
                    log.debug(clConfig.getWorkerAlias() + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
                }
            }
        }
    }

    private class OwnershipTreeChangeListener implements TreeCacheListener {

        @Override
        public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
            Boolean checkOwner = false;
            switch (treeCacheEvent.getType()){
                case NODE_ADDED:
//                    log.debug(workerAlias + ": Tree node added: " + ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath()));
                    checkOwner = true;
                    break;
                case NODE_UPDATED:
//                    log.debug(workerAlias+ ": Tree node updated: " + ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath()));
                    checkOwner = true;
                    break;
                case NODE_REMOVED:
//                    log.debug(workerAlias+ ": Tree node removed: " + ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath()));
                    checkOwner = true;
                    break;
            }
            if(checkOwner && !ownerLock.isAcquiredInThisProcess() && hasAffinityForJvm()){
                if(ownerLock.acquire(2, TimeUnit.SECONDS)){
                    ZookeeperJvmHandler.this.clClient.delete()
                            .deletingChildrenIfNeeded()
                            .forPath(clConfig.getRequestNodePath(jvmAlias));
                    log.debug(clConfig.getWorkerAlias() + " Get the ownership of affinity jvm from remote worker");
                }
            }
        }
    }
}
