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
import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * @author Tibor Kulcsar
 * @since <pre>May 25, 2016</pre>
 */
public class ConfigurationFixtures {

	public static Configuration goldenConfiguration(String connectionString){
		Configuration configuration = new HierarchicalConfiguration();
		configuration.addProperty("provider.classname", ZookeeperClusterService.class.getName());
		configuration.addProperty("zookeeper.workeralias", "worker_01");
		//configuration.addProperty("zookeeper.connectionstring", "10.189.33.100:2181\\,10.189.33.101:2181\\,10.189.33.102:2181");
		configuration.addProperty("zookeeper.connectionstring", connectionString);
		configuration.addProperty("zookeeper.timeout", 1000);
		configuration.addProperty("zookeeper.retry", 3);
		configuration.addProperty("zookeeper.heartbeatpath", "/jmxtrans/workers");
		configuration.addProperty("zookeeper.configpath", "/jmxtrans/jvms");
		return  configuration;
	}
}
