package com.googlecode.jmxtrans.cluster.zookeeper;

import org.apache.commons.configuration.Configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by kulcsart on 5/17/2016.
 *
 */
public class ZookeeperConfigBuilder {
    private String connectionString;
    private String workerAlias;
    private String heartBeatPath;
    private String configPath;
    private Integer connectTimeout;
    private Integer connectRetry;

    public ZookeeperConfigBuilder(){

    }

    public ZookeeperConfigBuilder withConnectionString(String connectionString){
        checkNotNull(connectionString, "Connection String cannot be null!");
        this.connectionString = connectionString;
        return this;
    }

    public ZookeeperConfigBuilder withWorkerAlias(String workerAlias){
        checkNotNull(workerAlias, "Worker Alias cannot be null!");
        this.workerAlias = workerAlias;
        return this;
    }

    public ZookeeperConfigBuilder withHeartBeatPath(String heartBeatPath){
        checkNotNull(heartBeatPath, "Heart beating path cannot be null!");
        this.heartBeatPath = heartBeatPath;
        return this;
    }

    public ZookeeperConfigBuilder withConfigPath(String configPath){
        checkNotNull(configPath, "Configurtation path cannot be null!");
        this.configPath = configPath;
        return this;
    }

    public ZookeeperConfigBuilder withConnectTimeout(int connectTimeout){
        checkArgument(connectTimeout > 100, "Connection timeout should be at least 100ms!");
        this.connectTimeout = connectTimeout;
        return this;
    }

    public ZookeeperConfigBuilder withConnectRetry(int connectRetry){
        checkArgument(connectRetry > 0, "Connection retry has to be >=1 !");
        this.connectRetry = connectRetry;
        return this;
    }

    public ZookeeperConfig build(){
        return new ZookeeperConfig(
                this.workerAlias,
                this.connectionString,
                this.heartBeatPath,
                this.configPath,
                this.connectTimeout,
                this.connectRetry
                );
    }

    public static ZookeeperConfig buildFromProperties(Configuration configuration){
        return new ZookeeperConfigBuilder()
                    .withWorkerAlias(configuration.getString("zookeeper.workeralias"))
                    .withConnectionString(configuration.getString("zookeeper.connectionstring"))
                    .withHeartBeatPath(configuration.getString("zookeeper.heartbeatpath"))
                    .withConfigPath(configuration.getString("zookeeper.configpath"))
                    .withConnectTimeout(configuration.getInt("zookeeper.timeout"))
                    .withConnectRetry(configuration.getInt("zookeeper.retry"))
                    .build();
    }
}
