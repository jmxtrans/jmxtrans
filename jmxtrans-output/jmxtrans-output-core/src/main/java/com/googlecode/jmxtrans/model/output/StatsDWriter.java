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
import com.googlecode.jmxtrans.model.results.CPrecisionValueTransformer;
import com.googlecode.jmxtrans.model.results.ValueTransformer;
import com.googlecode.jmxtrans.monitoring.ManagedGenericKeyedObjectPool;
import com.googlecode.jmxtrans.monitoring.ManagedObject;
import com.google.common.base.MoreObjects;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This output writer sends data to a host/port combination in the StatsD
 * format.
 *
 * @author neilh
 */
@EqualsAndHashCode(exclude = {"pool", "mbean"})
@ToString
public class StatsDWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(StatsDWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";
	private static final String BUCKET_TYPE = "bucketType";
	private static final String STRING_VALUE_AS_KEY = "stringValuesAsKey";
	private static final String STRING_VALUE_DEFAULT_COUNTER = "stringValueDefaultCount";
	private static final Pattern STATSD_INVALID = Pattern.compile("[:|]");

	private final ByteBuffer sendBuffer;

	private final String bucketType;
	private final String rootPrefix;
	private final String replacementForInvalidChar;
	private final InetSocketAddress address;
	private final DatagramChannel channel;
	private final Boolean stringsValuesAsKey;
	@Nonnull private final Long stringValueDefaultCount;

	private GenericKeyedObjectPool<SocketAddress, DatagramSocket> pool;
	private ManagedObject mbean;

	@Nonnull private final ValueTransformer valueTransformer = new CPrecisionValueTransformer();


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
			@JsonProperty(STRING_VALUE_AS_KEY) Boolean stringsValuesAsKey,
			@JsonProperty(STRING_VALUE_DEFAULT_COUNTER) Long stringValueDefaultCount,
			@JsonProperty("settings") Map<String, Object> settings,
			@JsonProperty("replacementForInvalidChar") String replacementForInvalidChar
			) throws IOException {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
		log.warn("StatsDWriter is deprecated. Please use StatsDWriterFactory instead.");
		channel = DatagramChannel.open();
		sendBuffer = ByteBuffer.allocate((short) 1500);

		// bucketType defaults to c == counter
		this.bucketType = firstNonNull(bucketType, (String) getSettings().get(BUCKET_TYPE), "c");
		this.rootPrefix = firstNonNull(rootPrefix, (String) getSettings().get(ROOT_PREFIX), "servers");
		// treat string attributes as key
		this.stringsValuesAsKey = firstNonNull(stringsValuesAsKey,
				(Boolean) getSettings().get(STRING_VALUE_AS_KEY), false);
		this.stringValueDefaultCount = firstNonNull(stringValueDefaultCount,
				(Long) getSettings().get(STRING_VALUE_DEFAULT_COUNTER), 1L);

		if (host == null) {
			host = (String) getSettings().get(HOST);
		}

		if (port == null) {
			port = Settings.getIntegerSetting(getSettings(), PORT, null);
		}
		this.replacementForInvalidChar = MoreObjects.firstNonNull(replacementForInvalidChar, "_");

		checkNotNull(host, "Host cannot be null");
		checkNotNull(port, "Port cannot be null");
		this.address = new InetSocketAddress(host, port);
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {}


	@Override
	public void start() throws LifecycleException {
		try {
			pool = new GenericKeyedObjectPool<>(new DatagramSocketFactory());
			pool.setTestOnBorrow(true);
			pool.setMaxActive(-1);
			pool.setMaxIdle(-1);
			pool.setTimeBetweenEvictionRunsMillis(MILLISECONDS.convert(5, MINUTES));
			pool.setMinEvictableIdleTimeMillis(MILLISECONDS.convert(5, MINUTES));

			this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool) pool, "StatsdConnectionPool");
			ManagementFactory.getPlatformMBeanServer()
					.registerMBean(this.mbean, this.mbean.getObjectName());
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	@Override
	public void close() throws LifecycleException {
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

	@Override
	public void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {

		List<String> typeNames = this.getTypeNames();

		for (Result result : results) {
			log.debug(result.toString());

			String key = KeyUtils.getKeyString(server, query, result, typeNames, rootPrefix);
			if (isNotValidValue(result.getValue())) {
				log.debug("Skipping message key[{}] with value: {}.", key, result.getValue());
				continue;
			}

			// These characters can mess with formatting.
			String line = STATSD_INVALID.matcher(key).replaceAll(this.replacementForInvalidChar)
				+ computeActualValue(result.getValue()) + "|" + bucketType + "\n";

			doSend(line.trim());
		}
	}

	private boolean isNotValidValue(Object value){
		return ! (isNumeric(value) || stringsValuesAsKey);
	}

	private String computeActualValue(Object value){
		Object transformedValue = valueTransformer.apply(value);
		if(isNumeric(transformedValue)){
			return ":" + transformedValue.toString();
		}

		return "." + transformedValue.toString() + ":" + stringValueDefaultCount.toString();
	}

	private synchronized boolean doSend(String stat) {
		try {
			log.debug("StatsD Message: " + stat);

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
			log.error("Could not send metrics to Statsd", e);
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
			log.error("Could not send metrics to Statsd", e);
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
