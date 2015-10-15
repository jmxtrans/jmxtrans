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

import com.google.common.base.MoreObjects;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * The Class ManagedGenericKeyedObjectPool.
 *
 * @author marcos.lois
 */
public class ManagedGenericKeyedObjectPool implements ManagedGenericKeyedObjectPoolMBean, ManagedObject {

	/** The object name. */
	private ObjectName objectName;

	/** The default pool name. */
	private final String poolName;

	/** The pool. */
	private final GenericKeyedObjectPool pool;

	/**
	 * The Constructor.
	 *
	 * @param pool     the pool
	 * @param poolName the pool name
	 */
	public ManagedGenericKeyedObjectPool(GenericKeyedObjectPool pool, String poolName) {
		this.poolName = MoreObjects.firstNonNull(poolName, "Noname");
		this.pool = pool;
	}

	/**
	 * Gets the pool name.
	 *
	 * @return the pool name
	 */
	public String getPoolName() {
		return poolName;
	}

	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		if (objectName == null) {
			objectName = new ObjectName("com.googlecode.jmxtrans:Type=GenericKeyedObjectPool,PoolName=" + this.poolName + ",Name=" + this.getClass().getSimpleName() + "@" + this.hashCode());
		}
		return objectName;
	}

	@Override
	public void setObjectName(ObjectName objectName) throws MalformedObjectNameException {
		this.objectName = objectName;
	}

	@Override
	public void setObjectName(String objectName) throws MalformedObjectNameException {
		this.objectName = ObjectName.getInstance(objectName);
	}

	@Override
	public int getMaxActive() {
		return pool.getMaxActive();
	}

	@Override
	public int getMaxIdle() {
		return pool.getMaxIdle();
	}

	@Override
	public long getMaxWait() {
		return pool.getMaxWait();
	}

	@Override
	public int getMinIdle() {
		return pool.getMinIdle();
	}

	@Override
	public int getNumActive() {
		return pool.getNumActive();
	}

	@Override
	public int getNumIdle() {
		return pool.getNumIdle();
	}

	@Override
	public void setMaxActive(int maxActive) {
		this.pool.setMaxActive(maxActive);
	}

	@Override
	public void setMaxIdle(int maxIdle) {
		this.pool.setMaxIdle(maxIdle);
	}

	@Override
	public void setMinIdle(int maxIdle) {
		this.pool.setMinIdle(maxIdle);
	}

	@Override
	public void setMaxWait(long maxWait) {
		this.pool.setMaxWait(maxWait);
	}
}
