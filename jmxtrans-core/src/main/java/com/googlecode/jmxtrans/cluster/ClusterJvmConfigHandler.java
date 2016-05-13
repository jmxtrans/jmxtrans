package com.googlecode.jmxtrans.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by kulcsart on 5/6/2016.
 *
 */
public class ClusterJvmConfigHandler{

    private static final Logger log = LoggerFactory.getLogger(ClusterJvmConfigHandler.class);

    private static final String OWNER_NODE_NAME  = "owner";
    private static final String AFFINITY_NODE_NAME = "affinity";
    private static final String CONFIG_NODE_NAME = "config";
    private static final String REQUEST_NODE_NAME = "request";

    private String heartbeatPath;
    private String jvmRootPath;

    private String jvmAlias;
    private String workerAlias;
    private String affinity = "";
    private String config = "";

    PathChildrenCache jvmConfigCache = null;

    TreeCache jvmOwnershipCache = null;

    InterProcessSemaphoreMutex lock = null;

    private ClusterConfigChangeListener changeListener;

    private CuratorFramework clClient;

    public ClusterJvmConfigHandler(String heartbeatPath, String configPath, String jvmAlias, String workerAlias, CuratorFramework clClient, ClusterConfigChangeListener changeListener){
        this.jvmAlias = jvmAlias;
        this.workerAlias = workerAlias;
        this.clClient = clClient;
        this.changeListener = changeListener;
        this.heartbeatPath = heartbeatPath;
        this.jvmRootPath = configPath + "/" + jvmAlias;
        log.debug("JVM handler " + this.workerAlias + " - " +this.jvmAlias + " initiated!");
    }

    public void initialize() throws Exception{
        try {
            lock = new InterProcessSemaphoreMutex(clClient,this.jvmRootPath + "/" + OWNER_NODE_NAME);
            jvmConfigCache = new PathChildrenCache(clClient, this.jvmRootPath, false);
            jvmOwnershipCache = new TreeCache(clClient, this.jvmRootPath + "/" + OWNER_NODE_NAME);

            if (null == this.clClient.checkExists().forPath(this.jvmRootPath)) {
                throw new ClusterJvmHandlerException("The JVM config does not exists in Zookeeper");
            }
            if(null == this.clClient.checkExists().forPath(this.jvmRootPath + "/" + AFFINITY_NODE_NAME)){
                throw new ClusterJvmHandlerException("The affinity cannot be determinded");
            }

            getRequiredFields();
            runLeaderElection();

            jvmConfigCache.getListenable().addListener(new ConfigPathChangeListener());
            jvmConfigCache.start();

            jvmOwnershipCache.getListenable().addListener(new OwnershipTreeChangeListener());
            jvmOwnershipCache.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getRequiredFields() throws Exception{
        getAffinmityFromZookeeper();
    }

    private void getAffinmityFromZookeeper() throws Exception{
        this.affinity = new String(this.clClient.getData().forPath(this.jvmRootPath + "/"  + AFFINITY_NODE_NAME));
    }

    private void getConfigFromZookeeper() throws Exception{
        if(this.lock.isAcquiredInThisProcess()){
            this.config = new String(this.clClient.getData().forPath(this.jvmRootPath + "/"  + CONFIG_NODE_NAME));
        }
        else
        {
            this.config = null;
        }
    }

    private void tryToTakeOwnership(Boolean sendRequestIfFailed){
        if(!takeOwnership()){
            if(sendRequestIfFailed) {
                setUpOwnershipRequest();
            }
        }
    }

    private Boolean takeOwnership(){
        try{
            return lock.acquire(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setUpOwnershipRequest(){
        try{
            Stat stat = this.clClient.checkExists().forPath(this.jvmRootPath + "/"  + REQUEST_NODE_NAME);
            if(null == stat) {
                this.clClient.create().withMode(CreateMode.EPHEMERAL).forPath(this.jvmRootPath + "/"  + REQUEST_NODE_NAME,
                        this.workerAlias.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void releaseOwnership(){
        try {
            log.debug("Release " + this.workerAlias + " " + this.jvmAlias + " owner:" + lock.isAcquiredInThisProcess());
            if(lock.isAcquiredInThisProcess()) {
                lock.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runLeaderElection(){
        if(lock.isAcquiredInThisProcess()){
            log.debug(this.workerAlias + "has the ownership for " + this.jvmAlias);
            return;
        }
        if(hasAffinityForJvm()){
            log.debug("Worker " + this.workerAlias + " tries to get the ownership to AFFINITY jvm " + this.jvmAlias);
            tryToTakeOwnership(true);
            return;
        }

        try {
            if(null == this.clClient.checkExists().forPath(heartbeatPath + "/" + this.affinity)){
                log.debug("Worker " + this.workerAlias + " tries to get the ownership to REMOTE jvm " + jvmAlias);
                tryToTakeOwnership(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Boolean hasAffinityForJvm(){
        return this.workerAlias.equals(this.affinity);
    }

    public void dipose(){

    }

    public void notifyWorkerpoolChange(){
        runLeaderElection();
    }

    private class OwnershipTreeChangeListener implements TreeCacheListener{

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
            if(checkOwner && !lock.isAcquiredInThisProcess() && hasAffinityForJvm()){
                if(lock.acquire(2, TimeUnit.SECONDS)){
                    ClusterJvmConfigHandler.this.clClient.delete()
                            .deletingChildrenIfNeeded()
                            .forPath(ClusterJvmConfigHandler.this.jvmRootPath + "/" + REQUEST_NODE_NAME);
                    log.debug(workerAlias + " Get the ownership of affinity jvm from remote worker");
                }
            }
        }
    }

    private class ConfigPathChangeListener implements PathChildrenCacheListener{

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            getRequiredFields();

            switch ( pathChildrenCacheEvent.getType() )
            {
                case CHILD_ADDED:
                    log.debug(workerAlias + " Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    if(REQUEST_NODE_NAME.equals(ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()))
                            && !hasAffinityForJvm()
                            && lock.isAcquiredInThisProcess())
                    {
                        log.debug("Affinity worker arrived for " + jvmAlias);
                        releaseOwnership();
                    }
                    break;

                case CHILD_UPDATED:
                {
                    log.debug(workerAlias + " Node changed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()) +
                            new String(clClient.getData().forPath(pathChildrenCacheEvent.getData().getPath())));


                    if(ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()).equals(AFFINITY_NODE_NAME)){
                        ClusterJvmConfigHandler.this.getAffinmityFromZookeeper();
                        break;
                    }

                    if(ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()).equals(CONFIG_NODE_NAME)){
                        ClusterJvmConfigHandler.this.getConfigFromZookeeper();
                        break;
                    }
                    break;
                }

                case CHILD_REMOVED:
                {
                    log.debug(workerAlias + " Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
                }
            }
        }
    }
}
