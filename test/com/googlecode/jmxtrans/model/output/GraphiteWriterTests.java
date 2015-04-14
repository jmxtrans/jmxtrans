package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.ConfigurationParser;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GraphiteWriterTests {

	@Test(expected = NullPointerException.class)
	public void hostIsRequired() throws ValidationException {
		try {
			GraphiteWriter.builder()
					.setPort(123)
					.build();
		} catch (NullPointerException npe) {
			assertThat(npe).hasMessage("Host cannot be null.");
			throw npe;
		}
	}

	@Test(expected = NullPointerException.class)
	public void portIsRequired() throws ValidationException {
		try {
			GraphiteWriter.builder()
					.setHost("localhost")
					.build();
		} catch (NullPointerException npe) {
			assertThat(npe).hasMessage("Port cannot be null.");
			throw npe;
		}
	}

	private static GraphiteWriter getGraphiteWriter(OutputStream out) throws Exception {
		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = mock(GenericKeyedObjectPool.class);
		Socket socket = mock(Socket.class);
		when(pool.borrowObject(any(InetSocketAddress.class))).thenReturn(socket);

		when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = GraphiteWriter.builder()
				.setHost("localhost")
				.setPort(2003)
				.build();
		writer.setPool(pool);

		return writer;
	}

	@Test
	public void writeSingleResult() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GraphiteWriter writer = getGraphiteWriter(out);

		writer.doWrite(server, query, of(result));

		// check that Graphite format is respected
		assertThat(out.toString()).startsWith("servers.host_123.classNameAlias.attributeName_key 1 ");
	}

	@Test
	public void useObjDomainWorks() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		// Set useObjDomain to true
		Query query = Query.builder().setUseObjDomainAsKey(true).build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)1));

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		GraphiteWriter writer = getGraphiteWriter(out);

		writer.doWrite(server, query, of(result));

		// check that Graphite format is respected
		assertThat(out.toString()).startsWith("servers.host_123.objDomain.attributeName_key 1 ");
	}
	
	@Test
	public void allowDottedWorks() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").setAlias("host").build();
		Query query = Query.builder().setAllowDottedKeys(true).build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)1));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		// Set useObjDomain to true
		GraphiteWriter writer = getGraphiteWriter(out);

		writer.doWrite(server, query, of(result));
		System.out.println(out.toString());
		// check that Graphite format is respected
		assertThat(out.toString()).startsWith("servers.host.className.attributeName.key 1 ");
	}

	@Test
	public void booleanAsNumberWorks() throws Exception {
		File testInput = new File(GraphiteWriterTests.class.getResource("/booleanTest.json").toURI());

		boolean continueOnJsonError = true;

		ImmutableList servers = new ConfigurationParser().parseServers(of(testInput), continueOnJsonError);

		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)true));

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = mock(GenericKeyedObjectPool.class);
		Socket socket = mock(Socket.class);
		when(pool.borrowObject(any(InetSocketAddress.class))).thenReturn(socket);

		when(socket.getOutputStream()).thenReturn(out);

		Server server = ((Server)servers.get(0));
		Query query = server.getQueries().asList().get(0);
		GraphiteWriter writer = (GraphiteWriter) (query.getOutputWriters().get(0));
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));

		// check that the booleanAsNumber property was picked up from the JSON
		assertThat(out.toString()).startsWith("servers.host_123.objDomain.attributeName.key 1");
	}


	@Test
	public void socketInvalidatedWhenError() throws Exception {
		// a lot of setup for not much of a test ...
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = mock(GenericKeyedObjectPool.class);
		Socket socket = mock(Socket.class);
		when(pool.borrowObject(any(InetSocketAddress.class))).thenReturn(socket);
		UnflushableByteArrayOutputStream out = new UnflushableByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = GraphiteWriter.builder()
				.setHost("localhost")
				.setPort(2003)
				.build();
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));
		verify(pool).invalidateObject(any(InetSocketAddress.class), eq(socket));
		verify(pool, never()).returnObject(any(InetSocketAddress.class), eq(socket));
	}

	private static class UnflushableByteArrayOutputStream extends ByteArrayOutputStream {
		@Override
		public void flush() throws IOException {
			throw new IOException();
		}
	}

}
