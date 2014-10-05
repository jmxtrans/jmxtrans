package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;


/**
 * {@link com.googlecode.jmxtrans.model.OutputWriter} for the <a href="https://github.com/OpenTSDB/tcollector/blob/master/collectors/0/udp_bridge.py">TCollector udp_bridge</a>.
 * Largely based on StatsDWriter and OpenTSDBWriter
 *
 * @author Kieren Hynd
 * @author Arthur Naseef
 */
public class TCollectorUDPWriter extends OpenTSDBGenericWriter {
	private static final Logger log = LoggerFactory.getLogger(TCollectorUDPWriter.class);

	protected SocketAddress address;
	protected DatagramSocket dgSocket;

	@JsonCreator
	public TCollectorUDPWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, debugEnabled, settings);
	}

	/**
	 * Do not add the hostname tag "host" with the name of the host by default since tcollector normally adds the
	 * hostname.
	 */
	@Override
	protected boolean getAddHostnameTagDefault() {
		return false;
	}

	/**
	 * Setup at start of the writer.
	 */
	@Override
	public void prepareSender() throws LifecycleException {

		if (host == null || port == null) {
			throw new LifecycleException("Host and port for " + this.getClass().getSimpleName() + " output can't be null");
		}

		try {
			this.dgSocket = new DatagramSocket();
			this.address = new InetSocketAddress(host, port);
		} catch (SocketException sockExc) {
			log.error("Failed to create a datagram socket", sockExc);
			throw new LifecycleException(sockExc);
		}
	}

	/**
	 * Send a single metric to TCollector.
	 *
	 * @param metricLine - the line containing the metric name, value, and tags for a single metric; excludes the
	 *                   "put" keyword expected by OpenTSDB and the trailing newline character.
	 */
	@Override
	protected void sendOutput(String metricLine) throws IOException {
		DatagramPacket packet;
		byte[] data;

		data = metricLine.getBytes("UTF-8");
		packet = new DatagramPacket(data, 0, data.length, this.address);

		this.dgSocket.send(packet);
	}

}
