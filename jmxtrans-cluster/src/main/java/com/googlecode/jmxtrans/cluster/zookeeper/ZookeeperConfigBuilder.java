/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.cluster.zookeeper;

import org.apache.commons.configuration.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ZookeeperConfigBuilder. The builder of ZookeeperConfig. It can build from code or from a Configuration object.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */

public class ZookeeperConfigBuilder {
	@Nullable private String connectionString;
	@Nullable private String workerAlias;
	@Nullable private String heartBeatPath;
	@Nullable private String configPath;
	@Nullable private Integer connectTimeout;
	@Nullable private Integer connectRetry;

	public ZookeeperConfigBuilder(){
	}

	public ZookeeperConfigBuilder withConnectionString(@Nonnull String connectionString){
		this.connectionString = connectionString;
		return this;
	}

	public ZookeeperConfigBuilder withWorkerAlias(@Nonnull String workerAlias){
		this.workerAlias = workerAlias;
		return this;
	}

	public ZookeeperConfigBuilder withHeartBeatPath(@Nonnull String heartBeatPath){
		this.heartBeatPath = heartBeatPath;
		return this;
	}

	public ZookeeperConfigBuilder withConfigPath(@Nonnull String configPath){
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
