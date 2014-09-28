package com.googlecode.jmxtrans.pool;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import javax.management.remote.JMXConnector;
import java.util.Map;

import com.googlecode.jmxtrans.connections.DatagramSocketFactory;
import com.googlecode.jmxtrans.connections.JmxConnectionFactory;
import com.googlecode.jmxtrans.connections.SocketFactory;
import com.googlecode.jmxtrans.model.Server;

import static com.google.common.collect.Maps.newHashMap;

public final class PoolUtils {
	public static final String SOCKET_FACTORY_POOL = SocketFactory.class.getSimpleName();
	public static final String JMX_CONNECTION_FACTORY_POOL = JmxConnectionFactory.class.getSimpleName();
	public static final String DATAGRAM_SOCKET_FACTORY_POOL = DatagramSocketFactory.class.getSimpleName();

	private PoolUtils() {}

	/**
	 * Gets the object pool. TODO: Add options to adjust the pools, this will be
	 * better performance on high load
	 *
	 * @param factory the factory
	 * @return the object pool
	 */
	public static <K, V> GenericKeyedObjectPool getObjectPool(KeyedPoolableObjectFactory<K, V> factory) {
		GenericKeyedObjectPool<K, V> pool = new GenericKeyedObjectPool<K, V>(factory);
		pool.setTestOnBorrow(true);
		pool.setMaxActive(-1);
		pool.setMaxIdle(-1);
		pool.setTimeBetweenEvictionRunsMillis(1000 * 60 * 5);
		pool.setMinEvictableIdleTimeMillis(1000 * 60 * 5);

		return pool;
	}

	/**
	 * Helper method which returns a default PoolMap.
	 * <p/>
	 * TODO: allow for more configuration options?
	 */
	public static Map<String, KeyedObjectPool> getDefaultPoolMap() {
		Map<String, KeyedObjectPool> poolMap = newHashMap();

		GenericKeyedObjectPool pool = getObjectPool(new SocketFactory());
		poolMap.put(SOCKET_FACTORY_POOL, pool);



		GenericKeyedObjectPool<Server, JMXConnector> jmxPool = getObjectPool(new JmxConnectionFactory());
		poolMap.put(JMX_CONNECTION_FACTORY_POOL, jmxPool);

		GenericKeyedObjectPool dsPool = getObjectPool(new DatagramSocketFactory());
		poolMap.put(DATAGRAM_SOCKET_FACTORY_POOL, dsPool);

		return poolMap;
	}
}
