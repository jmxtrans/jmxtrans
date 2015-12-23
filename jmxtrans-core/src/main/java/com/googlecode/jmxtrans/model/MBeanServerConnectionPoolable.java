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

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Poolable;
import stormpot.Slot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class MBeanServerConnectionPoolable implements Poolable {

	private static final Logger logger = LoggerFactory.getLogger(MBeanServerConnectionPoolable.class);

	@Nonnull private final Slot slot;

	@Nullable @Getter private final JMXConnector jmxConnector;

	@Nonnull @Getter private final MBeanServerConnection connection;

	public MBeanServerConnectionPoolable(@Nonnull Slot slot, JMXConnector jmxConnector, @Nonnull MBeanServerConnection connection) {
		this.slot = slot;
		this.jmxConnector = jmxConnector;
		this.connection = connection;
	}

	@Override
	public void release() {
		slot.release(this);
	}

	public void invalidate() {
		slot.expire(this);
	}
}
