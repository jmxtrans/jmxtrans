package com.googlecode.jmxtrans.connections;

import org.junit.Test;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class DatagramSocketFactoryTest {
	@Test
	public void testDatagramSocketFactoryMakeObject() throws Exception {
		int port = 50123;

		DatagramSocketFactory socketFactory = new DatagramSocketFactory();

		InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);

		DatagramSocket socketObject = socketFactory.makeObject(socketAddress);

		// Test if the remote address/port is the correct one.
		assertThat(socketObject.getPort()).isEqualTo(port);
		assertThat(socketObject.getInetAddress()).isEqualTo(Inet4Address.getLocalHost());
	}
}
