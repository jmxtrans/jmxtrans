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
package com.googlecode.jmxtrans.cluster;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration.Configuration;

import javax.annotation.Nonnull;

/**
 * ClusterServiceFactory. This factory hides the guice dependecy injection. It creates a ClusterService based on the
 * classname defined in the configuration.
 *
 * @author Tibor Kulcsar
 * @since <pre>May 17, 2016</pre>
 */
public class ClusterServiceFactory {

	@Nonnull
	private static Injector createInjector(@Nonnull Configuration configuration) throws ClassNotFoundException {
		return Guice.createInjector(
				new ClusterServiceModule(configuration));
	}

	@Nonnull
	public static ClusterService createClusterService(@Nonnull Configuration configuration) throws ClassNotFoundException{
		Injector injector = createInjector(configuration);
		return injector.getInstance(ClusterService.class);
	}
}
