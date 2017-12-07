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
package com.googlecode.jmxtrans.model;

import com.googlecode.jmxtrans.connections.JMXConnection;
import org.junit.Test;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JMXConnectionTest {

	@Test
	public void serverConnectionIsExposed() {
		MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);

		JMXConnection jmxConnection = new JMXConnection(null, mBeanServerConnection);

		assertThat(jmxConnection.getMBeanServerConnection()).isSameAs(mBeanServerConnection);
	}

	@Test
	public void connectorIsClosed() throws IOException {
		MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
		JMXConnector jmxConnector = mock(JMXConnector.class);
		JMXConnection jmxConnection = new JMXConnection(jmxConnector, mBeanServerConnection);

		jmxConnection.close();
		verify(jmxConnector).close();
	}

	@Test
	public void nullConnectorIsNotClosed() throws IOException {
		MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
		JMXConnection jmxConnection = new JMXConnection(null, mBeanServerConnection);

		jmxConnection.close();
	}
}
