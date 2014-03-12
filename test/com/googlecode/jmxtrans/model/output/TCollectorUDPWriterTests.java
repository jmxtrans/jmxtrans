package com.googlecode.jmxtrans.model.output;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.LifecycleException;

import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for {@link TCollectorUDPWriter}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TCollectorUDPWriter.class, DatagramSocket.class })
public class TCollectorUDPWriterTests {
	protected TCollectorUDPWriter	writer;
	protected Query			mockQuery;
	protected Result		mockResult;
	protected DatagramSocket	mockDgSocket;
	protected Logger		mockLog;
	protected Map<String, Object>	testValues;

	@Before
	public void	setupTest () throws Exception {
		this.mockQuery       = mock(Query.class);
		this.mockResult      = mock(Result.class);
		this.mockDgSocket    = mock(DatagramSocket.class);
		this.mockLog         = mock(Logger.class);


			//
			// Setup common mock interactions.
			//

		PowerMockito.whenNew(DatagramSocket.class).withAnyArguments().thenReturn(this.mockDgSocket);


			// When results are needed.

		when(this.mockQuery.getResults()).thenReturn(Arrays.asList(this.mockResult));
		testValues = new HashMap<String, Object>();
		testValues.put("x-att1-x", "120021");
		when(this.mockResult.getValues()).thenReturn(testValues);
		when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		when(this.mockResult.getTypeName()).thenReturn("Type=x-type-x");


			//
			// Prepare the object under test and test data.
			//

		this.writer = new TCollectorUDPWriter();
		this.writer.addSetting("host", "localhost");
		this.writer.addSetting("port", 8923);


			// Inject the mock logger

		Whitebox.setInternalState(TCollectorUDPWriter.class, Logger.class, this.mockLog);
	}

	@After
	public void	cleanupTest () {
	}

	/**
	 * Test a successful send to tcollector.
	 */
	@Test
	public void	testSuccessfulSend() throws Exception {
			//
			// Prepare
			//

		ArgumentCaptor<DatagramPacket>	packetCapture = ArgumentCaptor.forClass(DatagramPacket.class);

			//
			// Execute
			//

		this.writer.start();
		this.writer.doWrite(this.mockQuery);
		this.writer.stop();


			//
			// Verifications
			//

		verify(this.mockDgSocket).send(packetCapture.capture());

		String sentString = new String(packetCapture.getValue().getData(),
		                               packetCapture.getValue().getOffset(),
		                               packetCapture.getValue().getLength());

		assertThat(sentString, Matchers.startsWith("X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		assertThat(sentString, not(containsString("host=")));
	}

	/**
	 * Test a socket exception when creating the DatagramSocket.
	 */
	@Test
	public void	testSocketException () throws Exception {
			//
			// Prepare
			//

		SocketException	sockExc = new SocketException("X-SOCK-EXC-X");
		PowerMockito.whenNew(DatagramSocket.class).withNoArguments().thenThrow(sockExc);

		try {
				//
				// Execute
				//

			this.writer.start();

			fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {

				//
				// Verify
				//

			assertSame(sockExc, lcExc.getCause());
			verify(this.mockLog).error(contains("create a datagram socket"), eq(sockExc));
		}
	}
}
