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
package com.googlecode.jmxtrans.model.output.support;

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
public class UdpOutputWriterBuilderIT {

	@Rule public UdpLoggingServer udpLoggingServer = new UdpLoggingServer(UTF_8);

	@Test
	public void messageIsSent() throws Exception {
		WriterPoolOutputWriter<DummyWriterBasedOutputWriter> outputWriter = UdpOutputWriterBuilder.builder(
				udpLoggingServer.getLocalSocketAddress(),
				new DummyWriterBasedOutputWriter("message"))
				.build();

		outputWriter.doWrite(dummyServer(), dummyQuery(), dummyResults());
		outputWriter.close();

		await().atMost(500, MILLISECONDS).until(messageReceived("message"));
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
