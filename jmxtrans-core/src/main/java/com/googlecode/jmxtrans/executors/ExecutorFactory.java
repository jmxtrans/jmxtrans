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
package com.googlecode.jmxtrans.executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.googlecode.jmxtrans.monitoring.ManagedThreadPoolExecutor;

import javax.management.MalformedObjectNameException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ExecutorFactory {
	private final int defaultPoolSize;
	private final int defaultWorkQueueCapacity;
	private final String executorAlias;

	public ExecutorFactory(int defaultPoolSize, int defaultWorkQueueCapacity, String executorAlias) {
		this.defaultPoolSize = defaultPoolSize;
		this.defaultWorkQueueCapacity = defaultWorkQueueCapacity;
		this.executorAlias = executorAlias;
	}

	public ManagedThreadPoolExecutor create(String aliasSuffix) throws MalformedObjectNameException {
		final String serverAlias = aliasSuffix == null
				? executorAlias
				: String.format("%s-%s", executorAlias, aliasSuffix);
		ThreadFactory threadFactory = threadFactory(serverAlias);

		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(defaultWorkQueueCapacity);

		// each server can have different settings in separate thread pool strategy
		// this logic can be implemented in json/yaml configs if it will be needed
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(defaultPoolSize, defaultPoolSize, 0L, MILLISECONDS, workQueue, threadFactory);
		ManagedThreadPoolExecutor executorMBean = new ManagedThreadPoolExecutor(executor, serverAlias);

		return executorMBean;
	}

	private static ThreadFactory threadFactory(String alias) {
		return new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("jmxtrans-" + alias + "-%d")
				.build();
	}
}
