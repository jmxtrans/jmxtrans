package com.googlecode.jmxtrans.cluster;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
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
    private Map<String, ClusterJvmConfigHandler> jvmConfigs;

    private static final Logger log = LoggerFactory.getLogger(ClusterConfigClient.class);

    public ClusterConfigClient(){
        jvmConfigs = new HashMap<>();
    }

    public void initialize(File clConfigFile) throws ConfigurationException{
        this.clConfiguration = new PropertiesConfiguration(clConfigFile);
        initialize(clConfiguration.getString("cluster.connectionstring"),
                clConfiguration.getString("cluster.alias"));
    }

    public void initialize(String clServiceConnectionString, String workerAlias) throws ConfigurationException{
        if(null == clServiceConnectionString || "".equals(clServiceConnectionString)){
            throw new ConfigurationException("None or empty connectionString!");
        }
        if(null == workerAlias || "".equals(workerAlias)){
            throw new ConfigurationException("The worker alias cannot be null or empty! Maybe 'cluster.alias' " +
                    "filed is missing from the configuration");
        }
        this.clServiceConnectionString = clServiceConnectionString;
        this.workerAlias = workerAlias;
    }


    @Override
    public void run() {
        try{
            startClusterConnection();
            startHeartBeating();
            queryConfigs();

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
        Stat stat = this.clClient.checkExists().forPath("/jmxtrans/workers");
        if(null == stat){
            this.clClient.create()
                    .creatingParentContainersIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/jmxtrans/workers");
        }
        this.clClient.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath("/jmxtrans/workers/" + this.workerAlias);
    }

    private void stopHeartBeating() throws Exception{
        if(isHeartBeating()){
            this.clClient.delete()
                         .deletingChildrenIfNeeded()
                         .forPath("/jmxtrans/workers/" + this.workerAlias);
        }
    }

    private Boolean isHeartBeating() throws Exception {
        Stat stat = this.clClient.checkExists().forPath("/jmxtrans/workers" + this.workerAlias);
        return (null != stat);
    }

    private void queryConfigs() throws Exception {
        if (null == this.clClient.checkExists().forPath("/jmxtrans/jvms")) {
            throw new Exception("Znode is miissing! Have you started the cluster manager?");
        }
        List<String> jvms = this.clClient.getChildren().forPath("/jmxtrans/jvms");
        for (String i : jvms) {
            ClusterJvmConfigHandler handler = new ClusterJvmConfigHandler(i, this.workerAlias, this.clClient, this);
            handler.initialize();

            this.jvmConfigs.put(i, handler);

        }
    }
}
