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
import com.googlecode.jmxtrans.util.NumberUtils;
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This output writer sends data to a host/port combination in the StatsD
 * format.
 *
 * @author neilh
 */
public class StatsDWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(StatsDWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";
	private final ByteBuffer sendBuffer;

	private final String bucketType;
	private final String rootPrefix;
	private final InetSocketAddress address;
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
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("host") String host,
			@JsonProperty("port") Integer port,
			@JsonProperty("bucketType") String bucketType,
			@JsonProperty("rootPrefix") String rootPrefix,
			@JsonProperty("settings") Map<String, Object> settings) throws IOException {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		channel = DatagramChannel.open();
		sendBuffer = ByteBuffer.allocate((short) 1500);

		// bucketType defaults to c == counter
		this.bucketType = firstNonNull(bucketType, (String) getSettings().get(BUCKET_TYPE), "c");
		this.rootPrefix = firstNonNull(rootPrefix, (String) getSettings().get(ROOT_PREFIX), "servers");

		if (host == null) {
			host = (String) getSettings().get(HOST);
		}

		if (port == null) {
			port = Settings.getIntegerSetting(getSettings(), PORT, null);
		}
		checkNotNull(host, "Host cannot be null");
		checkNotNull(port, "Port cannot be null");
		this.address = new InetSocketAddress(host, port);
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
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

	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {

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

	public String getBucketType() {
		return bucketType;
	}

	public String getHostname() {
		return address.getHostName();
	}

	public int getPort() {
		return address.getPort();
	}
}
