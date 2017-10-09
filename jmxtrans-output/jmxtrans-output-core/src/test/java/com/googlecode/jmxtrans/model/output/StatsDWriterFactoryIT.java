/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.test.UdpLoggingServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.Callable;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Category({IntegrationTest.class, RequiresIO.class})
public class StatsDWriterFactoryIT {

	@Rule
	public UdpLoggingServer udpLoggingServer = new UdpLoggingServer(UTF_8);

	@Test
	public void messageIsSent() throws Exception {
		WriterPoolOutputWriter<StatsDWriter2> statsDWriter = new StatsDWriterFactory(
			ImmutableList.<String>of(),
				null, null, false, null,
				udpLoggingServer.getLocalSocketAddress().getHostName(),
				udpLoggingServer.getLocalSocketAddress().getPort(),
				null, null, null, null
		).create();

		statsDWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());
		statsDWriter.close();

		await().atMost(200, MILLISECONDS)
				.until(messageReceived("servers.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount:10|c\n"));
	}

	private Callable<Boolean> messageReceived(final String message) {
		return new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return udpLoggingServer.messageReceived(message);
			}
		};
	}


}
