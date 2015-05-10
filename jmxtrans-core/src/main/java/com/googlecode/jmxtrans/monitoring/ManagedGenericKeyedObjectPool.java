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
