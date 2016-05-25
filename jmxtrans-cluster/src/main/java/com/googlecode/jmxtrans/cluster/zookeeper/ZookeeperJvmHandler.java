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
    private AtomicReference<OwnerMode> ownerMode = new AtomicReference<OwnerMode>(OwnerMode.LEADER_ELECTION);
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

        configPathCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                handleConfigPathEvent(pathChildrenCacheEvent);
            }
        });

        configPathCache.start();
        startAffinityWorkerCache();
        updateOwnership();
    }

    protected void close() throws Exception{
        stopAffinity();
        stopLeaderLatch();
        configPathCache.close();
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

    private void stopAffinityWorkerCache() throws Exception{
        try{
            affinityWorkerCache.close();
        }
        finally{
            affinityWorkerCache = null;
        }
    }

    private void startLeaderLatch() throws Exception{
        if((null != leaderLatch) && (leaderLatch.getState() == LeaderLatch.State.STARTED)){ return; }
        stopLeaderLatch();
        leaderLatch = new LeaderLatch(clClient,clConfig.getOwnerNodePath(jvmAlias));
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() { getOwnership(); }

            @Override
            public void notLeader() { lostOwnership(); }
        });
        leaderLatch.start();
    }

    private void stopLeaderLatch() throws Exception{
        if((null != leaderLatch) && (leaderLatch.getState() != LeaderLatch.State.CLOSED)){
            leaderLatch.close();
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
        stopLeaderLatch();
        startAffinity();
    }

    private void switchToLeaderLatch() throws Exception{
        stopAffinity();
        startLeaderLatch();
    }

    private void getOwnership(){
        ownerState.set(OwnerState.OWNER);
        notifyConfigChange();

    }

    private void lostOwnership(){
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
            if(!affinity.isPresent() && !newAffinity.equals(affinity.get())) {
                affinity = Optional.of(newAffinity);
                affinityWorker = Optional.of(clClient.checkExists().forPath(clConfig.getAffinityWorkerPath(affinity.get())));
            }
        }

        if(affinityWorker.isPresent() && ownerMode.compareAndSet(OwnerMode.LEADER_ELECTION, OwnerMode.AFFINITY_WORKER)){
            switchToAffinity();
            return;
        }

        if(!affinityWorker.isPresent() && ownerMode.compareAndSet(OwnerMode.AFFINITY_WORKER, OwnerMode.LEADER_ELECTION)){
            switchToLeaderLatch();
        }
    }

    private boolean hasAffinityForJvm() throws Exception{
        return (affinity.isPresent() && clConfig.getWorkerAlias().equals(affinity.get()));
    }

    public void workerChanged() throws Exception{
        updateOwnership();
    }

    private enum OwnerMode{
        AFFINITY_WORKER,
        LEADER_ELECTION
    }

    private enum OwnerState{
        OWNER,
        NOT_OWNER
    }
}
