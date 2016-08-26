package com.googlecode.jmxtrans.cluster.zookeeper;

import com.google.common.base.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ZookeeperJvmHandler.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ZookeeperJvmHandler  {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperJvmHandler.class);

    private final String jvmAlias;
    private final CuratorFramework clClient;
    private final ZookeeperConfig clConfig;
    private final JvmConfigChangeListener listener;

    private String jmxTransConfig;
    private Optional<String> affinity = Optional.<String>absent();
    private AtomicReference<OwnerMode> ownerMode = new AtomicReference<OwnerMode>(OwnerMode.LATENT);
    private AtomicReference<OwnerState> ownerState = new AtomicReference<OwnerState>(OwnerState.NOT_OWNER);

    private PathChildrenCache   configPathCache;
    private NodeCache           affinityWorkerCache;
    private Optional<Stat>      affinityWorker;

    private LeaderLatch leaderLatch;
    private InterProcessSemaphoreMutex affinityLock;


    public ZookeeperJvmHandler(String jvmAlias,
                               CuratorFramework clClient,
                               ZookeeperConfig clConfig,
                               JvmConfigChangeListener listener) throws Exception{
        checkArgument(jvmAlias.length() > 0, "Jvm alias name cannot be null or empty!");
        this.jvmAlias = jvmAlias;
        this.clClient = checkNotNull(clClient, "Zookeeper client cannot be null!");
        this.clConfig = checkNotNull(clConfig, "Configuration cannot be null!");
        this.listener = listener;
        initialze();
    }

    private void initialze() throws Exception{
        configPathCache = new PathChildrenCache(clClient, clConfig.getJvmPath(this.jvmAlias), false);

        checkNotNull(this.clClient.checkExists().forPath(clConfig.getOwnerNodePath(this.jvmAlias)),
                "The JVM - %s clConfig does not exists in Zookeeper!", this.jvmAlias);
        checkNotNull(this.clClient.checkExists().forPath(clConfig.getJvmAffinityNodePath(this.jvmAlias)),
                "The affinity of Jvm %s cannot be determinded!", this.jvmAlias);


        affinityLock = new InterProcessSemaphoreMutex(clClient,clConfig.getJvmAffinityNodePath(jvmAlias));

        configPathCache.start();
        configPathCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                handleConfigPathEvent(pathChildrenCacheEvent);
            }
        });

        updateOwnership();
        startAffinityWorkerCache();
    }

    protected void close() throws Exception{
        ownerState.set(OwnerState.NOT_OWNER);
        ownerMode.set(OwnerMode.LATENT);
        configPathCache.close();
        stopAffinity();
        stopLeaderLatch();
        switchToLatent();
    }

    private void startAffinityWorkerCache() throws Exception{
        stopAffinityWorkerCache();
        if(this.affinity.isPresent()) {
            affinityWorkerCache = new NodeCache(clClient, clConfig.getAffinityWorkerPath(this.affinity.get()));
            affinityWorkerCache.getListenable().addListener(new NodeCacheListener() {
                @Override
                public void nodeChanged() throws Exception {
                    updateOwnership();
                }
            });
        }
    }

    private void stopAffinityWorkerCache(){
        try{
            affinityWorkerCache.close();
        } catch (IOException e) {
            log.warn("Affinity worker cache cannot be closed", e);
        } catch (NullPointerException e){
            log.warn("Affinity worker cache in null", e);
        } finally{
            affinityWorkerCache = null;
        }
    }

    private void startLeaderLatch() throws Exception{
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " start leader lection process");
        if((null != leaderLatch) && (leaderLatch.getState() == LeaderLatch.State.STARTED)){ return; }
        stopLeaderLatch();
        leaderLatch = new LeaderLatch(clClient,clConfig.getOwnerNodePath(jvmAlias), clConfig.getWorkerAlias());

        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " win the leader election");
                try {
                    log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias + " Current leader is " + leaderLatch.getLeader());
                }
                catch (Exception e){
                    log.error("Leader Latch is not available", e);
                }
                getOwnership();
            }

            @Override
            public void notLeader() {
                log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " lost the leader election");
                try {
                    log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias + " Current leader is " + leaderLatch.getLeader());
                }
                catch (Exception e){
                    log.error("Leader Latch is not available", e);
                }

                lostOwnership();
            }
        });
        leaderLatch.start();
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " started leader lection process");
    }

    private void stopLeaderLatch() throws Exception{
        try{
            leaderLatch.close();
        } catch (NullPointerException e ){
            log.error(clConfig.getWorkerAlias() + "::" + jvmAlias + e.getMessage());
        }
    }

    private void startAffinity() throws Exception{
        if(affinityLock.isAcquiredInThisProcess()){
            return;
        }
        if(hasAffinityForJvm() && affinityLock.acquire(1, TimeUnit.MINUTES)){
            getOwnership();
        }
    }

    private void stopAffinity() throws Exception {
        if (affinityLock.isAcquiredInThisProcess()) {
            affinityLock.release();
        }
    }

    private void switchToAffinity() throws Exception{
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " switch to affinity");
        stopLeaderLatch();
        if(ownerState.compareAndSet(OwnerState.OWNER, OwnerState.NOT_OWNER)){
            notifyConfigRemoved();
        }
        startAffinity();
    }

    private void switchToLeaderLatch() throws Exception{
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " switch to leader election");
        stopAffinity();
        if(ownerState.compareAndSet(OwnerState.OWNER, OwnerState.NOT_OWNER)){
            notifyConfigRemoved();
        }
        startLeaderLatch();
    }

    private void switchToLatent() throws Exception{
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " switch to latent");
        stopAffinity();
        stopLeaderLatch();
        ownerState.compareAndSet(OwnerState.OWNER, OwnerState.NOT_OWNER);
        notifyConfigRemoved();
    }

    private void getOwnership(){
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " get the ownership");
        ownerState.set(OwnerState.OWNER);
        notifyConfigChange();
    }

    private void lostOwnership(){
        log.debug(clConfig.getWorkerAlias() + "::" + jvmAlias+ " lost the ownership");
        ownerState.set(OwnerState.NOT_OWNER);
        notifyConfigRemoved();
    }


    private void handleConfigPathEvent(PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception{
        switch (pathChildrenCacheEvent.getType()){
            case CHILD_ADDED:
            case CHILD_UPDATED:

                break;
            case CHILD_REMOVED:

        }
    }

    private boolean readConfigFromZookeeper() throws Exception{
        if(!OwnerState.OWNER.equals(ownerState.get())){
            return false;
        }

        jmxTransConfig = new String(clClient.getData().forPath(clConfig.getConfigNodePath(jvmAlias)));
        return true;
    }

    private void notifyConfigChange(){
        try {
            if (readConfigFromZookeeper()) {
                listener.jvmConfigChanged(jvmAlias, jmxTransConfig);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    private void notifyConfigRemoved(){
        listener.jvmConfigRemoved(jvmAlias);
    }

    private void updateOwnership() throws Exception{
        String newAffinity = new String(clClient.getData().forPath(
                clConfig.getJvmAffinityNodePath(jvmAlias)));

        if(newAffinity.length()>0) {
            affinity = Optional.of(newAffinity);
            if(affinity.isPresent() ){
                Stat stat = clClient.checkExists().forPath(clConfig.getAffinityWorkerPath(affinity.get()));
                affinityWorker = null != stat ? Optional.of(stat) : Optional.<Stat>absent();
            }
        }

        if(affinityWorker.isPresent() && (
                        ownerMode.compareAndSet(OwnerMode.LEADER_ELECTION, OwnerMode.AFFINITY_WORKER) ||
                        ownerMode.compareAndSet(OwnerMode.LATENT, OwnerMode.AFFINITY_WORKER))){
            switchToAffinity();
            return;
        }

        if(!affinityWorker.isPresent() && (
                        ownerMode.compareAndSet(OwnerMode.AFFINITY_WORKER, OwnerMode.LEADER_ELECTION) ||
                        ownerMode.compareAndSet(OwnerMode.LATENT, OwnerMode.LEADER_ELECTION))){
            switchToLeaderLatch();
        }
    }

    private boolean hasAffinityForJvm() throws Exception{
        return (affinity.isPresent() && clConfig.getWorkerAlias().equals(affinity.get()));
    }

    public OwnerMode getOwnerMode(){
        return ownerMode.get();
    }

    public OwnerState getOwnerState(){
        return ownerState.get();
    }

    public void workerChanged() throws Exception{
        updateOwnership();
    }

    protected enum OwnerMode{
        LATENT,
        AFFINITY_WORKER,
        LEADER_ELECTION
    }

    protected enum OwnerState{
        OWNER,
        NOT_OWNER
    }
}
