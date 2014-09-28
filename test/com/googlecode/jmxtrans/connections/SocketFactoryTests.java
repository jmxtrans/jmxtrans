package com.googlecode.jmxtrans.connections;

import com.google.common.io.Closer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;

public class SocketFactoryTests {

	public static final int ECHO_SERVER_PORT = 4321;
	private TCPEchoServer echoServer;
	private InetSocketAddress echoServerAddress;

	@Before
	public void startEchoServer() {
		echoServer = new TCPEchoServer(ECHO_SERVER_PORT);
		echoServer.start();
		echoServerAddress = new InetSocketAddress("localhost", ECHO_SERVER_PORT);
	}

	@Test
	public void createdSocketIsValid() throws IOException {
		Closer closer = Closer.create();
		try {
			SocketFactory socketFactory = new SocketFactory();
			Socket socket = closer.register(socketFactory.makeObject(echoServerAddress));
			assertThat(socketFactory.validateObject(echoServerAddress, socket)).isTrue();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@Test
	public void nullSocketIsInvalid() {
		assertThat(new SocketFactory().validateObject(echoServerAddress, null)).isFalse();
	}

	@Test
	public void closedSocketIsInvalid() throws IOException {
		Closer closer = Closer.create();
		try {
			SocketFactory socketFactory = new SocketFactory();
			Socket socket = closer.register(socketFactory.makeObject(echoServerAddress));
			socket.close();
			assertThat(socketFactory.validateObject(echoServerAddress, socket)).isFalse();
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@After
	public void stopEchoServer() {
		echoServer.stop();
	}

}
