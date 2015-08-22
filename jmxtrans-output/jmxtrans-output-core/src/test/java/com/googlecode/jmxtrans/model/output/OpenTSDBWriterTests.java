package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTSDBWriter}.
 */

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
	@Test
	public void socketInvalidatedWhenError() throws Exception {
		// a lot of setup for not much of a test ...
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = Mockito.mock(GenericKeyedObjectPool.class);
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(pool.borrowObject(Matchers.any(InetSocketAddress.class))).thenReturn(socket);
		UnflushableByteArrayOutputStream out = new UnflushableByteArrayOutputStream();
		Mockito.when(socket.getOutputStream()).thenReturn(out);

		OpenTSDBWriter writer = OpenTSDBWriter.builder()
				.setHost("localhost")
				.setPort(4243)
				.build();
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));
		Mockito.verify(pool).invalidateObject(Matchers.any(InetSocketAddress.class), Matchers.eq(socket));
		Mockito.verify(pool, Mockito.never()).returnObject(Matchers.any(InetSocketAddress.class), Matchers.eq(socket));
	}

	
	private static OpenTSDBWriter getOpenTSDBWriter(OutputStream out) throws Exception {
		return getOpenTSDBWriter(out, new ArrayList<String>());
	}

	private static OpenTSDBWriter getOpenTSDBWriter(OutputStream out, List<String> typeNames) throws Exception {
		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = Mockito.mock(GenericKeyedObjectPool.class);
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(pool.borrowObject(Matchers.any(InetSocketAddress.class))).thenReturn(socket);

		Mockito.when(socket.getOutputStream()).thenReturn(out);

		OpenTSDBWriter writer = OpenTSDBWriter.builder()
				.setHost("localhost")
				.setPort(4243)
				.addTypeNames(typeNames)
				.build();
		writer.setPool(pool);

		return writer;
	}

	@Test
	public void writeSingleResult() throws Exception {
		Server server = Server.builder().setPid("1").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OpenTSDBWriter writer = getOpenTSDBWriter(out);

		writer.doWrite(server, query, of(result));

		// check that OpenTSDB format is respected
		assertThat(out.toString()).startsWith("put classNameAlias.attributeName ")
			.contains("type=key")
			.contains("host=")  // hostname is added by default
			.endsWith("\n");
	}

	private static class UnflushableByteArrayOutputStream extends ByteArrayOutputStream {
		@Override
		public void flush() throws IOException {
			throw new IOException();
		}
	}

}
