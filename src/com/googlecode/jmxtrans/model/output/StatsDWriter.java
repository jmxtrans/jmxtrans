package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.connections.DatagramSocketFactory;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.monitoring.ManagedGenericKeyedObjectPool;
import com.googlecode.jmxtrans.monitoring.ManagedObject;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This output writer sends data to a host/port combination in the StatsD
 * format.
 *
 * @author neilh
 */
public class StatsDWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(StatsDWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";
	private ByteBuffer sendBuffer;

	/** bucketType defaults to c == counter */
	private String bucketType = "c";
	private String rootPrefix = "servers";
	private SocketAddress address;
	private final DatagramChannel channel;

	private static final String BUCKET_TYPE = "bucketType";

	private GenericKeyedObjectPool<SocketAddress, DatagramSocket> pool;
	private ManagedObject mbean;


	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 *
	 * @throws IOException
	 */
	@JsonCreator
	public StatsDWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) throws IOException {
		super(typeNames, debugEnabled, settings);
		channel = DatagramChannel.open();
		setBufferSize((short) 1500);
	}

	public synchronized void setBufferSize(short packetBufferSize) {
		if (sendBuffer != null) {
			flush();
		}
		sendBuffer = ByteBuffer.allocate(packetBufferSize);
	}


	@Override
	public void start() throws LifecycleException {
		try {
			pool = new GenericKeyedObjectPool<SocketAddress, DatagramSocket>(new DatagramSocketFactory());
			pool.setTestOnBorrow(true);
			pool.setMaxActive(-1);
			pool.setMaxIdle(-1);
			pool.setTimeBetweenEvictionRunsMillis(1000 * 60 * 5);
			pool.setMinEvictableIdleTimeMillis(1000 * 60 * 5);

			this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool) pool, "StatsdConnectionPool");
			ManagementFactory.getPlatformMBeanServer()
					.registerMBean(this.mbean, this.mbean.getObjectName());
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	@Override
	public void stop() throws LifecycleException {
		try {
			if (this.mbean != null) {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				mbs.unregisterMBean(this.mbean.getObjectName());
				this.mbean = null;
			}
			if (this.pool != null) {
				pool.close();
				this.pool = null;
			}
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	/** */
	public void validateSetup(Server server, Query query) throws ValidationException {
		String host = (String) this.getSettings().get(HOST);
		Object portObj = this.getSettings().get(PORT);
		Integer port = null;
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

		// Read access to 'address' are synchronized. I'm not yet completely
		// clear on the threading model used by JmxTrans, we might be able to
		// reduce the number of locks. Still this synchronized block will not
		// be expensive and at least ensure correctness of code.
		synchronized (this) {
			this.address = new InetSocketAddress(host, port);
		}

		if (this.getSettings().containsKey(BUCKET_TYPE))
			bucketType = (String) this.getSettings().get(BUCKET_TYPE);
	}

	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {

		List<String> typeNames = this.getTypeNames();

		for (Result result : results) {
			if (isDebugEnabled()) {
				log.debug(result.toString());
			}

			Map<String, Object> resultValues = result.getValues();
			if (resultValues != null) {
				for (Entry<String, Object> values : resultValues.entrySet()) {
					if (NumberUtils.isNumeric(values.getValue())) {

						String line = KeyUtils.getKeyString(server, query, result, values, typeNames, rootPrefix)
								+ ":" + values.getValue().toString() + "|" + bucketType + "\n";

						if (isDebugEnabled()) {
							log.debug("StatsD Message: " + line.trim());
						}

						doSend(line.trim());
					}
				}
			}
		}
	}

	private synchronized boolean doSend(String stat) {
		try {
			final byte[] data = stat.getBytes("utf-8");

			// If we're going to go past the threshold of the buffer then flush.
			// the +1 is for the potential '\n' in multi_metrics below
			if (sendBuffer.remaining() < (data.length + 1)) {
				flush();
			}

			if (sendBuffer.position() > 0) { // multiple metrics are separated
				// by '\n'
				sendBuffer.put((byte) '\n');
			}

			sendBuffer.put(data); // append the data

			flush();
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public synchronized boolean flush() {
		try {
			final int sizeOfBuffer = sendBuffer.position();

			if (sizeOfBuffer <= 0) {
				return false;
			} // empty buffer

			// send and reset the buffer
			sendBuffer.flip();
			final int nbSentBytes = channel.send(sendBuffer, this.address);
			sendBuffer.limit(sendBuffer.capacity());
			sendBuffer.rewind();

			return sizeOfBuffer == nbSentBytes;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
