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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.ThreadPoolExecutor;

public class SeparateExecutorRepository implements ExecutorRepository {
	@Nonnull private final Hashtable<Server, ThreadPoolExecutor> repository;
	@Nonnull private final ExecutorFactory executorFactory;
	@Nonnull private final ArrayList<ManagedThreadPoolExecutor> mBeans;

	public SeparateExecutorRepository(ExecutorFactory executorFactory){
		this.executorFactory = executorFactory;
		this.repository = new Hashtable<>();
		mBeans = new ArrayList<>();
	}

	@Override
	public Collection<ThreadPoolExecutor> getExecutors() {
		return repository.values();
	}

	@Override
	public ThreadPoolExecutor getExecutor(Server server) {
		return repository.get(server);
	}

	@Override
	public Collection<ManagedThreadPoolExecutor> getMBeans() {
		return mBeans;
	}

	@Override
	public void put(Server server) throws MalformedObjectNameException {
		final ManagedThreadPoolExecutor managedThreadPoolExecutor = executorFactory.create(server.getId());
		repository.put(server, managedThreadPoolExecutor.getExecutor());
		mBeans.add(managedThreadPoolExecutor);
	}
}

