package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPool;
import com.googlecode.jmxtrans.jmx.ManagedObject;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.SocketFactory;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * This low latency and thread save output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a
 *      href="http://graphite.wikidot.com/getting-your-data-into-graphite">http://graphite.wikidot.com/getting-your-data-into-graphite</a>
 *
 * @author jon
 */
public class GraphiteWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";
	
	private String host;
	private Integer port;
	private String rootPrefix = "servers";

	private static KeyedObjectPool pool = null;
	private static AtomicInteger activeServers = new AtomicInteger(0);

	private Lock statusLock = new ReentrantLock();
	private Condition statusConditionStarted = statusLock.newCondition();	
	private Condition statusConditionStopped = statusLock.newCondition();
	private GraphiteWriterSatus status = GraphiteWriterSatus.STOPPED;
	
	private ManagedObject mbean;
	private InetSocketAddress address;

	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 */
	public GraphiteWriter() { }

	/**
	 * If pool already started, just increase number of activeServers,
	 * if it is not, start the pool and register it on JMX
	 */
	@Override
	public void start() throws LifecycleException {
		statusLock.lock();
		try {
			if (status != GraphiteWriterSatus.STOPPED) {
				throw new LifecycleException("GraphiteWriter instance should be stopped");
			}
		
			int activeServersLocal = activeServers.incrementAndGet();
		
			// Start the pool when the first instance starts
			if (activeServersLocal == 1) {
				startPool();
			}
		
			status = GraphiteWriterSatus.STARTED;
			statusConditionStarted.signal();
		} finally {
			statusLock.unlock();
		}
	}
	
	@Override
	public void stop() throws LifecycleException {
		statusLock.lock();
		try {
			if (status != GraphiteWriterSatus.STARTED) {
				throw new LifecycleException("GraphiteWriter instance shold be started");
			}
		
			// Stop the pool if there is no more started instances
			if(activeServers.decrementAndGet() == 0) { 
				stopPool();
			}
		
			status = GraphiteWriterSatus.STOPPED;
			statusConditionStopped.signal();
		} finally {
			statusLock.unlock();
		}
	}
		
	/** */
	public void validateSetup(Query query) throws ValidationException {
		host = (String) this.getSettings().get(HOST);
		Object portObj = this.getSettings().get(PORT);
		if (portObj instanceof String) {
			port = Integer.parseInt((String) portObj);
		} else if (portObj instanceof Integer) {
			port = (Integer) portObj;
		}

		if (host == null || port == null) {
			throw new ValidationException("Host and port can't be null", query);
		}

		String rootPrefixTmp = (String) this.getSettings().get(ROOT_PREFIX);
		if (rootPrefixTmp != null) {
			rootPrefix = rootPrefixTmp;
		}

		this.address = new InetSocketAddress(host, port);
	}

	/** */
	public void doWrite(Query query) throws Exception {
		Socket socket = null;
		statusLock.lock();
		try {
			while(status == GraphiteWriterSatus.STARTING) {
				statusConditionStarted.await();
			}
			if (status != GraphiteWriterSatus.STARTED) {
				throw new LifecycleException("GraphiteWriter instance shold be started");
			}
			socket = (Socket) pool.borrowObject(address);
		} finally {
			statusLock.unlock();
		}

		try {
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

			List<String> typeNames = this.getTypeNames();

			for (Result result : query.getResults()) {
				if (isDebugEnabled()) {
					log.debug(result.toString());
				}
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						if (JmxUtils.isNumeric(values.getValue())) {
							StringBuilder sb = new StringBuilder();

							sb.append(JmxUtils.getKeyString(query, result, values, typeNames, rootPrefix));

							sb.append(" ");
							sb.append(values.getValue().toString());
							sb.append(" ");
							sb.append(result.getEpoch() / 1000);
							sb.append("\n");

							String line = sb.toString();
							if (isDebugEnabled()) {
								log.debug("Graphite Message: " + line.trim());
							}
							writer.write(line);
							writer.flush();
						}
					}
				}
			}
		} finally {
			pool.returnObject(address, socket);
		}
	}
	
	/**
	 * Starts the pool and register it with JMX
	 * 
	 * @throws LifecycleException
	 */
	private void startPool() throws LifecycleException {
		pool = JmxUtils.getObjectPool(new SocketFactory());
		
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName objectName = new ObjectName("com.googlecode.jmxtrans:Type=GenericKeyedObjectPool,PoolName=SocketFactory,Name=GWManagedGenericKeyedObjectPool");

			if (!mbs.isRegistered(objectName) ) {
				this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool)pool, Server.SOCKET_FACTORY_POOL);
				this.mbean.setObjectName(objectName);
				JmxUtils.registerJMX(this.mbean);
			}
			log.debug("GraptiteWriter connection pool is started");
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	/**
	 * Stops the pool and removes it from JMX
	 * 
	 * @throws LifecycleException
	 */
	private void stopPool() throws LifecycleException {
		try {
			if (this.mbean != null) {
				JmxUtils.unregisterJMX(this.mbean);
				this.mbean = null;
			}
			if (pool != null) {
				pool.close();
				pool = null;
			}
			log.debug("GraptiteWriter connection pool is stopped");
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

}
