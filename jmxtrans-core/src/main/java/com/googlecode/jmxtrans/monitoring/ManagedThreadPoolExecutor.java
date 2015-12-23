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
package com.googlecode.jmxtrans.monitoring;

import javax.annotation.Nonnull;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ManagedThreadPoolExecutor {

	@Nonnull private final ThreadPoolExecutor executor;
	private ObjectName objectName;

	public ManagedThreadPoolExecutor(@Nonnull ThreadPoolExecutor executor, @Nonnull String name) throws MalformedObjectNameException {
		this.executor = executor;
		this.objectName = new ObjectName("com.googlecode.jmxtrans:Type=ThreadPoolExecutor,PoolName=" + name);
	}

	public boolean allowsCoreThreadTimeOut() {
		return executor.allowsCoreThreadTimeOut();
	}

	public int getActiveCount() {
		return executor.getActiveCount();
	}

	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}

	public int getCorePoolSize() {
		return executor.getCorePoolSize();
	}

	public long getKeepAliveTimeSeconds() {
		return executor.getKeepAliveTime(SECONDS);
	}

	public int getLargestPoolSize() {
		return executor.getLargestPoolSize();
	}

	public int getMaximumPoolSize() {
		return executor.getMaximumPoolSize();
	}

	public int getPoolSize() {
		return executor.getPoolSize();
	}

	public long getTaskCount() {
		return executor.getTaskCount();
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public boolean isTerminating() {
		return executor.isTerminating();
	}

	public int workQueueRemainingCapacity() {
		return executor.getQueue().remainingCapacity();
	}

	public int workQueueSize() {
		return executor.getQueue().size();
	}

	public ObjectName getObjectName() {
		return objectName;
	}
}
