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

import com.googlecode.jmxtrans.cluster.ClusterService;
import com.googlecode.jmxtrans.cluster.ClusterServiceFactory;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClusterServiceFactory Tester.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 25, 2016</pre>
 */
public class ClusterServiceFactoryTest {

	/**
	 * Test the classname based dependency injection.
	 * @throws Exception
	 */
	@Test
	public void testDependencyInjection() throws Exception {
		ClusterService service = ClusterServiceFactory.createClusterService(
				ConfigurationFixtures.goldenConfiguration("127.0.0.1:2181"));

		assertThat(service).isExactlyInstanceOf(ZookeeperClusterService.class);
	}

	@Test(expected = ClassNotFoundException.class)
	public void nonExistentImplementationClassCausesException() throws Exception{
		Configuration configuration =  ConfigurationFixtures.goldenConfiguration("127.0.0.1:2181");
		configuration.setProperty("provider.classname", "non.existent.Clazz");

		ClusterServiceFactory.createClusterService(configuration);
	}
}
