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
package com.googlecode.jmxtrans.model;

import stormpot.Allocator;
import stormpot.Slot;

import javax.annotation.Nonnull;
import javax.management.remote.JMXConnector;

public class MBeanServerConnectionAllocator implements Allocator<MBeanServerConnectionPoolable> {

	@Nonnull private final Server server;

	public MBeanServerConnectionAllocator(@Nonnull Server server) {
		this.server = server;
	}

	@Override
	public MBeanServerConnectionPoolable allocate(Slot slot) throws Exception {
		if (server.isLocal()) {
			return new MBeanServerConnectionPoolable(slot, null, server.getLocalMBeanServer());
		} else {
			JMXConnector connection = server.getServerConnection();
			return new MBeanServerConnectionPoolable(slot, connection, connection.getMBeanServerConnection());
		}
	}

	@Override
	public void deallocate(MBeanServerConnectionPoolable poolable) throws Exception {
		JMXConnector connector = poolable.getJmxConnector();
		if (connector != null) connector.close();
	}
}
