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
package com.googlecode.jmxtrans.test;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.kaching.platform.testing.AllowExternalProcess;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import org.junit.rules.ExternalResource;

import javax.annotation.Nullable;
import java.net.URL;
import java.net.URLClassLoader;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

@AllowExternalProcess
public class MonitorableApp extends ExternalResource {

	private Process app;
	private final int jmxPort;

	public MonitorableApp(int jmxPort) {
		this.jmxPort = jmxPort;
	}

	@Override
	@IgnoreJRERequirement // ProcessBuilder.inheritIO() was introduced in Java 7. As this is only used in test, let's ignore it.
	protected void before() throws Throwable {
		app = new ProcessBuilder().command(
				"java", "-cp", getCurrentClasspath(),
				"-Dcom.sun.management.jmxremote.authenticate=false",
				"-Dcom.sun.management.jmxremote.port=" + jmxPort,
				"-Dcom.sun.management.jmxremote.ssl=false",
				DummyApp.class.getCanonicalName()
		)
				.inheritIO()
				.start();
	}

	private String getCurrentClasspath() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader)cl).getURLs();

		return  Joiner.on(":")
				.join(
						from(asList(urls))
								.transform(new Function<URL, String>() {
									@Nullable
									@Override
									public String apply(URL input) {
										return input.toString();
									}
								}));
	}

	@Override
	protected void after() {
		app.destroy();
	}
}
