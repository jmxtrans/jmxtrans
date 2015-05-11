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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;


/**
 * Tests for {@link OpenTSDBWriter}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpenTSDBWriter.class })
public class OpenTSDBWriterTests {
	protected OpenTSDBWriter writer;
	protected Query mockQuery;
	protected Result mockResult;
	protected Socket mockSocket;
	protected DataOutputStream mockOut;
	protected InputStreamReader mockInStreamRdr;
	protected BufferedReader mockBufRdr;
	protected Logger mockLog;
	protected ImmutableMap<String, Object> testValues;

	/**
	 * Prepare the test with standard mocks and mock interactions.  Also perform the base configuration of the
	 * object under test.
	 */
	@Before
	public void	setupTest () throws Exception {
		// Prepare Mock Objects
		this.mockQuery = Mockito.mock(Query.class);
		this.mockResult = Mockito.mock(Result.class);
		this.mockSocket = Mockito.mock(Socket.class);
		this.mockInStreamRdr = Mockito.mock(InputStreamReader.class);
		this.mockBufRdr = Mockito.mock(BufferedReader.class);
		this.mockLog = Mockito.mock(Logger.class);

		// PowerMockito mocks for those final/static classes and methods:
		this.mockOut = PowerMockito.mock(DataOutputStream.class);


		// Setup common mock interactions.
		PowerMockito.whenNew(Socket.class).withParameterTypes(String.class, int.class).
			withArguments("localhost", 4242).thenReturn(this.mockSocket);
		PowerMockito.whenNew(DataOutputStream.class).withAnyArguments().thenReturn(this.mockOut);
		PowerMockito.whenNew(InputStreamReader.class).withAnyArguments().thenReturn(this.mockInStreamRdr);
		PowerMockito.whenNew(BufferedReader.class).withAnyArguments().thenReturn(this.mockBufRdr);


		// When results are needed.
		testValues = ImmutableMap.<String, Object>of("x-att1-x", "120021");
		Mockito.when(this.mockResult.getValues()).thenReturn(testValues);
		Mockito.when(this.mockResult.getAttributeName()).thenReturn("X-ATT-X");
		Mockito.when(this.mockResult.getClassName()).thenReturn("X-DOMAIN.PKG.CLASS-X");
		Mockito.when(this.mockResult.getTypeName()).thenReturn("Type=x-type-x");
		Mockito.when(this.mockInStreamRdr.ready()).thenReturn(false);


		// Prepare the object under test and test data.
		this.writer = OpenTSDBWriter.builder()
				.setDebugEnabled(false)
				.setHost("localhost")
				.setPort(4242)
				.build();

		// Inject the mock logger
		Whitebox.setInternalState(OpenTSDBWriter.class, Logger.class, this.mockLog);
	}

	/**
	 * Test a successful send without any response from OpenTSDB.
	 */
	@Test
	public void	testSuccessfulSendNoOpenTSDBResponse () throws Exception {
		ArgumentCaptor<String>	lineCapture = ArgumentCaptor.forClass(String.class);

		// Execute.
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();

		// Verify.
		Mockito.verify(this.mockOut).writeBytes(lineCapture.capture());

		Assert.assertThat(lineCapture.getValue(), Matchers.startsWith("put X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		Assert.assertThat(lineCapture.getValue(), Matchers.containsString(" host="));
	}

	/**
	 * Verify a successful send with a response from OpenTSDB with the second ready() on the InputStream returning
	 * false.
	 */
	@Test
	public void	testSuccessfulSendOpenTSDBResponse1 () throws Exception {
		// Prepare.
		Mockito.when(this.mockInStreamRdr.ready()).thenReturn(true).thenReturn(false);
		Mockito.when(this.mockBufRdr.readLine()).thenReturn("X-OPENTSDB-MSG-X");

		// Execute.
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();


		// Verify.
		Mockito.verify(this.mockOut).writeBytes(Mockito.startsWith("put X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		Mockito.verify(this.mockLog).warn("OpenTSDB says: X-OPENTSDB-MSG-X");
	}

	/**
	 * Verify a successful send with a response from OpenTSDB with the second readLine returning null.
	 */
	@Test
	public void	testSuccessfulSendOpenTSDBResponse2 () throws Exception {
		// Prepare.
		Mockito.when(this.mockInStreamRdr.ready()).thenReturn(true);
		Mockito.when(this.mockBufRdr.readLine()).thenReturn("X-OPENTSDB-MSG-X").thenReturn(null);

		// Execute.
		this.writer.start();
		this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
		this.writer.stop();


		// Verify.
		Mockito.verify(this.mockOut).writeBytes(Mockito.startsWith("put X-DOMAIN.PKG.CLASS-X.X-ATT-X 0 120021"));
		Mockito.verify(this.mockLog).warn("OpenTSDB says: X-OPENTSDB-MSG-X");
	}

	/**
	 * Test throwing of UnknownHostException when creating the socket.
	 */
	@Test
	public void	testUnknownHostException () throws Exception {
		// Prepare.
		UnknownHostException uhExc = new UnknownHostException("X-TEST-UHE-X");
		PowerMockito.whenNew(Socket.class).withParameterTypes(String.class, int.class).
			withArguments("localhost", 4242).thenThrow(uhExc);

		try {
			// Execute.
			this.writer.start();

			Assert.fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {
			// Verify.
			Assert.assertSame(uhExc, lcExc.getCause());
			Mockito.verify(this.mockLog).error(contains("opening socket"), eq(uhExc));
		}
	}

	/**
	 * Test throwing of IOException when creating the socket.
	 */
	@Test
	public void	testSocketCreationIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		PowerMockito.whenNew(Socket.class).withParameterTypes(String.class, int.class).
			withArguments("localhost", 4242).thenThrow(ioExc);

		try {
			// Execute.
			this.writer.start();
			Assert.fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {
			// Verify.
			Assert.assertSame(ioExc, lcExc.getCause());
			Mockito.verify(this.mockLog).error(contains("opening socket"), eq(ioExc));
		}
	}

	/**
	 * Test throwing of IOException when closing the socket.
	 */
	@Test
	public void	testSocketCloseIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		Mockito.doThrow(ioExc).when(this.mockSocket).close();

		// Execute.
		this.writer.start();
		try {
			this.writer.stop();
			Assert.fail("LifecycleException missing");
		} catch ( LifecycleException lcExc ) {
			// Verify.
			Assert.assertSame(ioExc, lcExc.getCause());
			Mockito.verify(this.mockLog).error(contains("closing socket"), eq(ioExc));
		}
	}

	/**
	 * Test throwing of IOException when starting output.
	 */
	@Test
	public void	testStartOutputIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		Mockito.when(this.mockSocket.getOutputStream()).thenThrow(ioExc);

		// Execute.
		this.writer.start();
		try {
			this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
			Assert.fail("IOException missing");
		} catch ( IOException ioCaught ) {
			// Verify.
			Assert.assertSame(ioExc, ioCaught);
			Mockito.verify(this.mockLog).error(contains("output stream"), eq(ioExc));
		}
	}

	/**
	 * Test IOException thrown on SendOutput.
	 */
	@Test
	public void	testSendOutputIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		Mockito.doThrow(ioExc).when(this.mockOut).writeBytes(anyString());

		// Execute.
		this.writer.start();
		try {
			this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
			Assert.fail("IOException missing");
		} catch ( IOException ioCaught ) {
			// Verify.
			Assert.assertSame(ioExc, ioCaught);
			Mockito.verify(this.mockLog).error(contains("writing result"), eq(ioExc));
		}
	}

	@Test
	public void	testFinishOutputIOException () throws Exception {
		// Prepare.
		IOException	ioExc = new IOException("X-TEST-IO-EXC-X");
		Mockito.doThrow(ioExc).when(this.mockOut).flush();

		// Execute.
		this.writer.start();
		try {
			this.writer.doWrite(null, this.mockQuery, ImmutableList.of(this.mockResult));
			Assert.fail("exception on flush was not thrown");
		} catch ( IOException ioCaught ) {
			// Verify.
			Assert.assertSame(ioExc, ioCaught);
			Mockito.verify(this.mockLog).error(contains("flush failed"), eq(ioExc));
		}
	}

	@Test(expected = NullPointerException.class)
	public void	exceptionThrownIfHostIsNotDefined() throws Exception {
		OpenTSDBWriter.builder()
				.setPort(4242)
				.build();
	}

	@Test(expected = NullPointerException.class)
	public void	exceptionThrownIfPortIsNotDefined() throws Exception {
		OpenTSDBWriter.builder()
				.setHost("localhost")
				.build();
	}
}
