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
package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;


/**
 * Tests for {@link TCollectorUDPWriter}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TCollectorUDPWriter.class, DatagramSocket.class})
public class TCollectorUDPWriterTests {
	protected TCollectorUDPWriter writer;
	protected Query mockQuery;
	protected Result mockResult;
	protected DatagramSocket mockDgSocket;
	protected Logger mockLog;
	protected ImmutableMap<String, Object> testValues;

	@Before
	public void setupTest() throws Exception {
		this.mockQuery = Mockito.mock(Query.class);
		this.mockResult = Mockito.mock(Result.class);
		this.mockDgSocket = Mockito.mock(DatagramSocket.class);
		this.mockLog = Mockito.mock(Logger.class);


		// Setup common mock interactions.
		PowerMockito.whenNew(DatagramSocket.class).withAnyArguments().thenReturn(this.mockDgSocket);

		// When results are needed.
		testValues = ImmutableMap.<String, Object>of("x-att1-x", "120021");
		Mockito.when(this.mockResult.getValues()).thenReturn(testValues);
		Mockito.when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		Mockito.when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		Mockito.when(this.mockResult.getTypeName()).thenReturn("Type=x-type-x");


		// Prepare the object under test and test data.
		Map<String, Object> settings = newHashMap();
		settings.put("host", "localhost");
		settings.put("port", 8923);

		this.writer = TCollectorUDPWriter.builder()
				.setDebugEnabled(false)
				.setHost("localhost")
				.setPort(1234)
				.build();

		// Inject the mock logger
		Whitebox.setInternalState(TCollectorUDPWriter.class, Logger.class, this.mockLog);
	}

	@Test
	public void successfullySendMessageToTCollector() throws Exception {
		// Prepare
		ArgumentCaptor<DatagramPacket> packetCapture = ArgumentCaptor.forClass(DatagramPacket.class);

		// Execute
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();

		// Verifications
		Mockito.verify(this.mockDgSocket).send(packetCapture.capture());

		String sentString = new String(packetCapture.getValue().getData(),
				packetCapture.getValue().getOffset(),
				packetCapture.getValue().getLength());

		Assert.assertThat(sentString, Matchers.startsWith("X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		Assert.assertThat(sentString, Matchers.not(Matchers.containsString("host=")));
	}

	/**
	 * Test a socket exception when creating the DatagramSocket.
	 */
	@Test
	public void testSocketException() throws Exception {
		// Prepare
		SocketException sockExc = new SocketException("X-SOCK-EXC-X");
		PowerMockito.whenNew(DatagramSocket.class).withNoArguments().thenThrow(sockExc);

		try {
			// Execute
			this.writer.start();

			Assert.fail("LifecycleException missing");
		} catch (LifecycleException lcExc) {
			// Verify
			Assert.assertSame(sockExc, lcExc.getCause());
			Mockito.verify(this.mockLog).error(contains("create a datagram socket"), eq(sockExc));
		}
	}

	@Test(expected = NullPointerException.class)
	public void exceptionIsThrownWhenHostIsNotDefined() throws Exception {
		TCollectorUDPWriter.builder()
				.setPort(1234)
				.build();
	}

	@Test(expected = NullPointerException.class)
	public void exceptionIsThrownWhenPortIsNotDefined() throws Exception {
		TCollectorUDPWriter.builder()
				.setHost("localhost")
				.build();
	}
}
