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

<<<<<<< HEAD
import com.googlecode.jmxtrans.cluster.ClusterService;
import jdk.nashorn.internal.ir.annotations.Immutable;
=======
>>>>>>> 988d07c6c608d660654ed18a6232f2e7d68a38e8
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * ZookeeperConfig. This class stores the Zookeeper related configuration parts.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
@Immutable
public class ZookeeperConfig {
	public static final String OWNER_NODE_NAME  = "owner";
	public static final String AFFINITY_NODE_NAME = "affinity";
	public static final String CONFIG_NODE_NAME = "config";
	public static final String REQUEST_NODE_NAME = "request";

<<<<<<< HEAD
    @Getter private String connectionString;
    @Getter private String workerAlias;
    @Getter private String heartBeatPath;
    @Getter private String configPath;
    @Getter private int connectTimeout;
    @Getter private int connectRetry;
=======
	@Nonnull @Getter private final String connectionString;
	@Nonnull @Getter private final String workerAlias;
	@Nonnull @Getter private final String heartBeatPath;
	@Nonnull @Getter private final String configPath;
	@Nonnull @Getter private final int connectTimeout;
	@Nonnull @Getter private final int connectRetry;
>>>>>>> 988d07c6c608d660654ed18a6232f2e7d68a38e8

	protected ZookeeperConfig(
			@Nonnull String workerAlias,
			@Nonnull String connectionString,
			@Nonnull String heartBeatPath,
			@Nonnull String configPath,
			@Nonnull int connectTimeout,
			@Nonnull int connectRetry) {
		this.workerAlias = workerAlias;
		this.connectionString = connectionString;
		this.heartBeatPath = heartBeatPath;
		this.configPath = configPath;
		this.connectTimeout = connectTimeout;
		this.connectRetry = connectRetry;
	}

	public String getWorkerNodePath(){
		return this.heartBeatPath + "/" + workerAlias;
	}

	public String getJvmPath(String jvmAlias){
		return configPath + "/" + jvmAlias;
	}

	public String getOwnerNodePath(String jvmAlias){
		return configPath + "/" + jvmAlias + "/" + OWNER_NODE_NAME;
	}

	public String getJvmAffinityNodePath(String jvmAlias){
		return configPath + "/" + jvmAlias + "/" + AFFINITY_NODE_NAME;
	}

	public String getConfigNodePath(String jvmAlias){
		return configPath + "/" + jvmAlias + "/" + CONFIG_NODE_NAME;
	}

	public String getRequestNodePath(String jvmAlias){
		return configPath + "/" + jvmAlias + "/" + REQUEST_NODE_NAME;
	}

	public String getAffinityWorkerPath(String affinity){
		return this.heartBeatPath + "/" + affinity;
	}


}
