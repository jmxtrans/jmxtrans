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

import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.monitoring.ManagedThreadPoolExecutor;

import javax.annotation.Nonnull;
import javax.management.MalformedObjectNameException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;

public class CommonExecutorRepository implements ExecutorRepository {

	@Nonnull
	private final ManagedThreadPoolExecutor managedThreadPoolExecutor;

	public CommonExecutorRepository(ExecutorFactory executorFactory) throws MalformedObjectNameException {
		this.managedThreadPoolExecutor = executorFactory.create(null);
	}

	@Override
	public void put(Server server) throws MalformedObjectNameException {
		//nothing here because every server uses common thread pool
	}

	@Override
	public Collection<ThreadPoolExecutor> getExecutors() {
		return Arrays.asList(managedThreadPoolExecutor.getExecutor());
	}

	@Override
	public ThreadPoolExecutor getExecutor(Server server) {
		return managedThreadPoolExecutor.getExecutor();
	}

	@Override
	public Collection<ManagedThreadPoolExecutor> getMBeans() {
		return Arrays.asList(managedThreadPoolExecutor);
	}

	@Override
	public void remove(Server server) {
		//nothing here because every server uses common thread pool
	}
}
