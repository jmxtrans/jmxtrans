package com.googlecode.jmxtrans.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * The Class ManagedGenericKeyedObjectPool.
 * 
 * @author marcos.lois
 */
public class ManagedGenericKeyedObjectPool implements ManagedGenericKeyedObjectPoolMBean, ManagedObject {
    
    /** The object name. */
    private ObjectName objectName;
    
    /** The default pool name. */
    private String poolName = "Noname";
    
    /** The pool. */
    private GenericKeyedObjectPool pool;
    
	/**
	 * The Constructor.
	 *
	 * @param pool the pool
	 */
	public ManagedGenericKeyedObjectPool(GenericKeyedObjectPool pool) {
		this.pool = pool;
	}

    /**
     * The Constructor.
     *
     * @param pool the pool
     * @param poolName the pool name
     */
    public ManagedGenericKeyedObjectPool(GenericKeyedObjectPool pool, String poolName) {
    	if(poolName != null)
    		this.poolName = poolName;
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

	/**
	 * Sets the pool name.
	 *
	 * @param poolName the pool name
	 */
	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedObject#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            objectName = new ObjectName("com.googlecode.jmxtrans:Type=GenericKeyedObjectPool,PoolName=" + this.poolName + ",Name=" + this.getClass().getSimpleName() + "@" + this.hashCode());
        }
        return objectName;
    }

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedObject#setObjectName(javax.management.ObjectName)
	 */
	@Override
    public void setObjectName(ObjectName objectName) throws MalformedObjectNameException {
        this.objectName = objectName;
    }

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedObject#setObjectName(java.lang.String)
	 */
	@Override
    public void setObjectName(String objectName) throws MalformedObjectNameException {
        this.objectName = ObjectName.getInstance(objectName);
    }

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#getMaxActive()
	 */
	@Override
	public int getMaxActive() {
		return pool.getMaxActive();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#getMaxIdle()
	 */
	@Override
	public int getMaxIdle() {
		return pool.getMaxIdle();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#getMaxWait()
	 */
	@Override
	public long getMaxWait() {
		return pool.getMaxWait();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#getMinIdle()
	 */
	@Override
	public int getMinIdle() {
		return pool.getMinIdle();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#getNumActive()
	 */
	@Override
	public int getNumActive() {
		return pool.getNumActive();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#getNumIdle()
	 */
	@Override
	public int getNumIdle() {
		return pool.getNumIdle();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#setMaxActive(int)
	 */
	@Override
	public void setMaxActive(int maxActive) {
		this.pool.setMaxActive(maxActive);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#setMaxIdle(int)
	 */
	@Override
	public void setMaxIdle(int maxIdle) {
		this.pool.setMaxIdle(maxIdle);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#setMinIdle(int)
	 */
	@Override
	public void setMinIdle(int maxIdle) {
		this.pool.setMinIdle(maxIdle);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPoolMBean#setMaxWait(long)
	 */
	@Override
	public void setMaxWait(long maxWait) {
		this.pool.setMaxWait(maxWait);
	}
}
