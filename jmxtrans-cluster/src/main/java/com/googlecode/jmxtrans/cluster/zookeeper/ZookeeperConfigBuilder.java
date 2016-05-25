package com.googlecode.jmxtrans.cluster.zookeeper;

import org.apache.commons.configuration.Configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ZookeeperConfigBuilder. The builder of ZookeeperConfig. It can build from code or from a Configuration object.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
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
        this.connectionString = checkNotNull(connectionString, "Connection String cannot be null!");
        return this;
    }

    public ZookeeperConfigBuilder withWorkerAlias(String workerAlias){
        this.workerAlias = checkNotNull(workerAlias, "Worker Alias cannot be null!");
        return this;
    }

    public ZookeeperConfigBuilder withHeartBeatPath(String heartBeatPath){
        this.heartBeatPath = checkNotNull(heartBeatPath, "Heart beating path cannot be null!");
        return this;
    }

    public ZookeeperConfigBuilder withConfigPath(String configPath){
        this.configPath = checkNotNull(configPath, "Configurtation path cannot be null!");
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
                checkNotNull(this.workerAlias, "workerAlias cannot be null!"),
                checkNotNull(this.connectionString, "connectionString string cannot be null!"),
                checkNotNull(this.heartBeatPath, "heartBeatPath string cannot be null!"),
                checkNotNull(this.configPath, "configPath string cannot be null!"),
                checkNotNull(this.connectTimeout, "connectTimeout string cannot be null!"),
                checkNotNull(this.connectRetry, "connectRetry string cannot be null!")
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
