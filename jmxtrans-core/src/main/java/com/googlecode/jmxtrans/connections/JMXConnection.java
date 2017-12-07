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
package com.googlecode.jmxtrans.connections;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ToString
@ThreadSafe
public class JMXConnection implements Closeable {
	@Nullable private final JMXConnector connector;
	@Nonnull @Getter private final MBeanServerConnection mBeanServerConnection;
	@Nonnull private final ExecutorService executor;
	private boolean markedAsDestroyed;
	private static final Logger logger = LoggerFactory.getLogger(JMXConnection.class);

	public JMXConnection(@Nullable JMXConnector connector, @Nonnull MBeanServerConnection mBeanServerConnection) {
		this.connector = connector;
		this.mBeanServerConnection = mBeanServerConnection;
		this.markedAsDestroyed = false;

		executor = Executors.newSingleThreadExecutor();
	}

	public boolean isAlive(){
		return !markedAsDestroyed;
	}

	@Override
	public void close() throws IOException {
		markedAsDestroyed = true;
		if (connector != null) {
			executor.submit(new Runnable() {
				public void run() {
					if (connector != null) {
						try {
							connector.close();
						} catch (IOException e) {
							logger.error("Error occurred during close connection {}", this, e);
						}
					}
				}
			});
		}
	}
}
