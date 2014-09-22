/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.jmxtrans.util;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Map;

/**
 * Represents a connection to a local {@link MBeanServerConnection}
 */
public class LocalJMXConnector implements JMXConnector {
	private final MBeanServerConnection serverConnection;

	public LocalJMXConnector(MBeanServerConnection serverConnection) {
		this.serverConnection = serverConnection;
	}

	public void connect() throws IOException {
	}

	public void connect(Map<String, ?> env) throws IOException {
	}

	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return serverConnection;
	}

	public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
		return serverConnection;
	}

	public void close() throws IOException {
	}

	public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		// TODO
	}

	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		// TODO
	}

	public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
		// TODO
	}

	public String getConnectionId() throws IOException {
		return "LocalJMXConnector";
	}
}
