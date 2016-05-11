package com.googlecode.jmxtrans.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
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

    private String jvmAlias;
    private String workerAlias;
    private String affinity = "";
    private String config = "";

    private Boolean isOwner = false;

    PathChildrenCache configPathCache = null;
    PathChildrenCache ownerPathCache  = null;
    InterProcessMutex lock = null;

    private ClusterConfigChangeListener changeListener;

    private CuratorFramework clClient;

    public ClusterJvmConfigHandler(String jvmAlias, String workerAlias, CuratorFramework clClient, ClusterConfigChangeListener changeListener){
        this.jvmAlias = jvmAlias;
        this.workerAlias = workerAlias;
        this.clClient = clClient;
        this.changeListener = changeListener;
        log.debug("JVM handler " + this.jvmAlias + " initiated!");
    }

    public void initialize() throws Exception{
        try {
            lock  = new InterProcessMutex(clClient, "/jmxtrans/jvms/" + this.jvmAlias + "/owner" );
            configPathCache = new PathChildrenCache(clClient, "/jmxtrans/jvms/" + this.jvmAlias, false);
            ownerPathCache  = new PathChildrenCache(clClient, "/jmxtrans/jvms/" + this.jvmAlias + "/owner", false);

            if (null == this.clClient.checkExists().forPath("/jmxtrans/jvms/" + this.jvmAlias)) {
                throw new ClusterJvmHandlerException("The JVM config does not exists in Zookeeper");
            }
            if(null == this.clClient.checkExists().forPath("/jmxtrans/jvms/" + this.jvmAlias + "/affinity")){
                throw new ClusterJvmHandlerException("The affinity cannot be determinded");
            }

            getRequiredFields();
            runLeaderElection();

            configPathCache.getListenable().addListener(new ConfigPathChangeListener());
            ownerPathCache.getListenable().addListener(new OwnerShipPathChangeListener());
            configPathCache.start();
            ownerPathCache.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getRequiredFields() throws Exception{
        this.affinity = new String(this.clClient.getData().forPath("/jmxtrans/jvms/" + this.jvmAlias + "/affinity"));

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
            Stat stat = this.clClient.checkExists().forPath("/jmxtrans/jvms/" + this.jvmAlias + "/request");
            if(null == stat) {
                this.clClient.create().withMode(CreateMode.EPHEMERAL).forPath("/jmxtrans/jvms/" + this.jvmAlias + "/request");
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
            log.debug("Worker " + this.workerAlias + " tries to get the ownership to affinity jvm");
            tryToTakeOwnership(true);
            return;
        }

        try {
            if(null == this.clClient.checkExists().forPath("/jmxtrans/workers/" + this.affinity)){
                log.debug("Worker " + this.workerAlias + " tries to get the ownership to REMOTE jvm");
                tryToTakeOwnership(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Boolean hasAffinityForJvm(){
        return this.workerAlias.equals(this.affinity);
    }

    private class OwnerShipPathChangeListener implements PathChildrenCacheListener{

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            if(!isOwner && lock.isAcquiredInThisProcess()){
                log.debug("Worker " + workerAlias + " has taken the ownership of " + jvmAlias);
                //config = new String(clClient.getData().forPath(""));
                isOwner = true;
            }
            else if(isOwner && ! lock.isAcquiredInThisProcess()){
                log.debug("Worker " + workerAlias + " has lost the ownership of " + jvmAlias);
                isOwner = false;
            }

        }
    }

    private class ConfigPathChangeListener implements PathChildrenCacheListener{

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            getRequiredFields();
            log.debug("JVM_" + jvmAlias + " affinity_" + affinity + " ownership:" + lock.isAcquiredInThisProcess());

            switch ( pathChildrenCacheEvent.getType() )
            {
                /*case CHILD_ADDED:
                {
                    System.out.println("Node added: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
                }*/

                case CHILD_UPDATED:
                {
                    System.out.println("Node changed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()) +
                            new String(clClient.getData().forPath(pathChildrenCacheEvent.getData().getPath())));
                    break;
                }

                case CHILD_REMOVED:
                {
                    System.out.println("Node removed: " + ZKPaths.getNodeFromPath(pathChildrenCacheEvent.getData().getPath()));
                    break;
                }
            }
        }
    }

}
