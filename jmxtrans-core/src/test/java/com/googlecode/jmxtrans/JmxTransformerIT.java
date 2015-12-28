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
package com.googlecode.jmxtrans;

import com.google.inject.Injector;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.test.MonitorableApp;
import com.googlecode.jmxtrans.test.OutputCapture;
import com.kaching.platform.testing.AllowDNSResolution;
import com.kaching.platform.testing.AllowLocalFileAccess;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@AllowDNSResolution
@AllowLocalFileAccess(paths = "*")
public class JmxTransformerIT {
	@Rule public OutputCapture output = new OutputCapture();
	@Rule public MonitorableApp app = new MonitorableApp(12345);
	private JmxTransformer jmxTransformer;

	@Before
	public void startJmxTrans() throws LifecycleException, URISyntaxException {
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		configuration.setRunPeriod(1);
		configuration.setJsonFile(file("integration-test.json"));
		Injector injector = JmxTransModule.createInjector(configuration);
		jmxTransformer = injector.getInstance(JmxTransformer.class);
		jmxTransformer.start();
	}

	private File file(String filename) throws URISyntaxException {
		return new File(getClass().getClassLoader().getResource(filename).toURI());
	}

	@Test
	public void metricsAreSentToStdout() throws Exception {
		await().atMost(2, SECONDS).until(output.stdoutHasLineContaining("Value=1"));
		await().atMost(2, SECONDS).until(output.stdoutHasLineContaining("Value=2"));
	}

	@After
	public void stopJmxTrans() throws LifecycleException {
		jmxTransformer.stop();
	}
}
