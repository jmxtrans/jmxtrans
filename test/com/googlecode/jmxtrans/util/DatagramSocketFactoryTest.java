package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.model.Query;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Simon Effenberg Date: 2013-05-16
 */
public class DatagramSocketFactoryTest {
	@Test
	public void testDatagramSocketFactoryMakeObject() {

    int port = 50123;

    BaseKeyedPoolableObjectFactory socketFactory = new DatagramSocketFactory();

    Object socketAddress = (Object) new InetSocketAddress(Inet4Address.getLocalhost(), port);

    Socket socketObject = (Socket) socketFactory.makeObject(socketAddress);

    // Test if the remote address/port is the correct one.
    assertEquals(port, socketObject.getPort());
    assertEquals(Inet4Address.getLocalhost(), socketObject.getInetAddess());
	}
}
