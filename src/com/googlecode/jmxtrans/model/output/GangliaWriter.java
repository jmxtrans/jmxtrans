package com.googlecode.jmxtrans.model.output;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * A cool writer for Ganglia. This is heavily inspired and somewhat copied from
 * code in Hadoop (GangliaContext).
 *
 * @author jon
 */
public class GangliaWriter extends BaseOutputWriter {

	private enum Slope {
		ZERO, POSITIVE, NEGATIVE, BOTH;

		private static Slope fromName(String slopeName) {
			for (Slope slope : values())
				if (slopeName.equalsIgnoreCase(slope.name()))
					return slope;
			return BOTH;
		}
	}

	private enum DataType {
		INT("int32"), DOUBLE("double"), STRING("string");

		private static DataType forValue(Object value) {
			if (value instanceof Integer || value instanceof Byte || value instanceof Short)
				return INT;
			if (value instanceof Long || value instanceof Float || value instanceof Double)
				return DOUBLE;

			// Convert to double or int if possible
			try {
				Double.parseDouble(value.toString());
				return DOUBLE;
			} catch (NumberFormatException e) {
				// Not a double	
			}
			try {
				Integer.parseInt(value.toString());
				return INT;
			} catch (NumberFormatException e) {
				// Not an int
			}

			return STRING;
		}

		private final String typeName;

		private DataType(String typeName) {
			this.typeName = typeName;
		}

		public String getTypeName() {
			return typeName;
		}

		public String asString(Object value) {
			return value == null ? "" : value.toString();
		}
	}

	private static final Pattern PATTERN_HOST_IP = Pattern.compile("(.+):([^:]+)$");

	private static final Logger log = LoggerFactory.getLogger(GangliaWriter.class);

	private static final String DEFAULT_UNITS = "";
	private static final Slope DEFAULT_SLOPE = Slope.BOTH;
	private static final int DEFAULT_TMAX = 60;
	private static final int DEFAULT_DMAX = 0;
	private static final int DEFAULT_PORT = 8649;
	private static final int BUFFER_SIZE = 1500; // as per libgmond.c
	private static final int DEFAULT_SEND_METADATA = 30;

	public static final String GROUP_NAME = "groupName";
	public static final String SLOPE = "slope";
	public static final String UNITS = "units";
	public static final String DMAX = "dmax";
	public static final String TMAX = "tmax";
	public static final String SEND_METADATA = "sendMetadata";

	protected byte[] buffer = new byte[BUFFER_SIZE];
	protected int offset;

	private Map<String, KeyedObjectPool> poolMap;
	private KeyedObjectPool pool;
	private Map<MetricMetaData, Integer> emittedMetadata = new HashMap<MetricMetaData, Integer>();
	private InetSocketAddress address;
	private String groupName;
	private String spoofedHostname;
	private Slope slope = DEFAULT_SLOPE;
	private String units = DEFAULT_UNITS;
	private int tmax = DEFAULT_TMAX;
	private int dmax = DEFAULT_DMAX;
	private int sendMetadata = DEFAULT_SEND_METADATA;

	/** */
	public GangliaWriter() {
		this.poolMap = JmxUtils.getDefaultPoolMap();
	}

	/** */
	public GangliaWriter(Map<String, KeyedObjectPool> poolMap) {
		this.poolMap = poolMap;
	}

	/**
	 * Allows one to set the object pool for socket connections to graphite
	 */
	@Override
	public void setObjectPoolMap(Map<String, KeyedObjectPool> poolMap) {
		this.poolMap = poolMap;
	}

	/**
	 * Validates the host/port and does some other setup.
	 */
	public void validateSetup(Query query) throws ValidationException {
		Integer port = getIntegerSetting(PORT, DEFAULT_PORT);
		String host = getStringSetting(HOST, null);
		if (host == null) {
			throw new ValidationException("Host can't be null", query);
		}
		// Construct the InetAddress of the gmond and the DataGram socket pool
		address = new InetSocketAddress(host, port);
		pool = this.poolMap.get(Server.DATAGRAM_SOCKET_FACTORY_POOL);

		// Determine the spoofed hostname
		spoofedHostname = getSpoofedHostName(query.getServer().getHost(), query.getServer().getAlias());

		// Get metric meta-data
		groupName = getStringSetting(GROUP_NAME, null);
		units = getStringSetting(UNITS, DEFAULT_UNITS);
		slope = Slope.fromName(getStringSetting(SLOPE, DEFAULT_SLOPE.name()));
		tmax = getIntegerSetting(TMAX, DEFAULT_TMAX);
		dmax = getIntegerSetting(DMAX, DEFAULT_DMAX);
		sendMetadata = getIntegerSetting(SEND_METADATA, DEFAULT_SEND_METADATA);

		log.debug("validated ganglia metric -- address: " + host + ":" + port + ", spoofed host: " + spoofedHostname + ", group: " + groupName
				+ ", units: " + units + ", slope:" + slope + ", tmax: " + tmax + ", dmax: " + dmax + ", sendMetadata: " + sendMetadata);
	}

	/**
	 * Determines the spoofed host name to be used when emitting metrics to a
	 * gmond process. Spoofed host names are of the form IP:hostname.
	 *
	 * @param host
	 *            the host of the gmond (ganglia monitor) to which we are
	 *            connecting, not null
	 * @param alias
	 *            the custom alias supplied, may be null
	 * @return the host name to use when emitting metrics, in the form of
	 *         IP:hostname
	 */
	public static String getSpoofedHostName(String host, String alias) {
		// Determine the host name to use as the spoofed host name, this should
		// be of the format IP:hostname
		String spoofed = host;
		if (StringUtils.isNotEmpty(alias)) {
			// If the alias was supplied in the appropriate format, use it
			// directly
			Matcher hostIpMatcher = PATTERN_HOST_IP.matcher(alias);
			if (hostIpMatcher.matches())
				return alias;
			// Otherwise, use the alias as the host
			spoofed = alias;
		}
		// Attempt to find the IP of the given host (this may be an aliased
		// host)
		try {
			return InetAddress.getByName(spoofed).getHostAddress() + ":" + spoofed;
		} catch (UnknownHostException e) {
			// ignore failure to resolve spoofed host
		}
		// Attempt to return the local host IP with the spoofed host name
		try {
			return InetAddress.getLocalHost().getHostAddress() + ":" + spoofed;
		} catch (UnknownHostException e) {
			// ignore failure to resolve spoofed host
		}
		// We failed to resolve the spoofed host or our local host, return "x"
		// for the IP
		return "x:" + spoofed;
	}

	public void doWrite(Query query) throws Exception {
		DatagramSocket socket = (DatagramSocket) pool.borrowObject(address);
		try {

			List<String> typeNames = this.getTypeNames();

			for (Result result : query.getResults()) {
				if (isDebugEnabled()) {
					log.debug(result.toString());
				}
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						// Find the data type of this value
						DataType type = DataType.forValue(values.getValue());
						if (type != null) {
							emitMetric(socket, spoofedHostname, JmxUtils.getKeyString2(query, result, values, typeNames, null), type, values
									.getValue().toString());
						}
					}
				}
			}

		} finally {
			pool.returnObject(address, socket);
		}
	}

	protected void emitMetric(DatagramSocket socket, String hostName, String metricName, DataType type, String value) throws IOException {
		if (metricName == null) {
			log.warn("Metric was emitted with no name.");
			return;
		} else if (value == null) {
			log.warn("Metric name " + metricName + " was emitted with a null value.");
			return;
		} else if (type == null) {
			log.warn("Metric name " + metricName + ", value " + value + " has no type.");
			return;
		}

		// The following XDR recipe was done through a careful reading of
		// gm_protocol.x in Ganglia 3.1 and carefully examining the output of
		// the gmetric utility with strace.

		// Send the metric metadata if it is time.
		maybeSendMetricMetadata(socket, hostName, metricName, type);

		// Now we send out a message with the actual value.
		offset = 0; // reset the offset
		xdr_int(133); // we are sending a string value
		xdr_string(hostName); // hostName
		xdr_string(metricName); // metric name
		xdr_int(1); // spoof = True
		xdr_string("%s"); // format field
		xdr_string(type.asString(value)); // metric value

		socket.send(new DatagramPacket(buffer, offset, address));

		log.debug("Emitted metric " + metricName + ", type " + type + ", value " + value + " for host: " + hostName);
	}

	private void maybeSendMetricMetadata(DatagramSocket socket, String hostName, String name, DataType type) throws IOException {
		MetricMetaData metaData = new MetricMetaData(hostName, name, type);

		Integer emittedSamples = emittedMetadata.get(metaData);
		if (emittedSamples == null)
			emittedSamples = 0;

		if (emittedSamples % sendMetadata == 0) {
			sendMetricMetadata(socket, metaData);
			log.debug("Emmitted metric metadata: " + metaData.toString());
		}

		emittedMetadata.put(metaData, emittedSamples + 1);
	}

	private void sendMetricMetadata(DatagramSocket socket, MetricMetaData metaData) throws IOException {
		// First we send out a metadata message
		offset = 0;
		xdr_int(128); // metric_id = metadata_msg
		xdr_string(metaData.hostName); // hostname
		xdr_string(metaData.metricName); // metric name
		xdr_int(1); // spoof = True
		xdr_string(metaData.type.getTypeName()); // metric type
		xdr_string(metaData.metricName); // metric name
		xdr_string(units); // units
		xdr_int(slope.ordinal()); // slope see gmetric.c
		xdr_int(tmax); // tmax, the maximum time between metrics
		xdr_int(dmax); // dmax, the maximum time (in seconds) to store the
						// metric value, 0 = forever

		/*
		 * Num of the entries in extra_value field for Ganglia 3.1.x
		 */
		if (StringUtils.isNotEmpty(groupName)) {
			xdr_int(1); // Indicates more meta-data is present
			xdr_string("GROUP"); /* Group attribute */
			xdr_string(groupName); /* Group value */
		} else {
			xdr_int(0); // Indicates no group name, end of packet
		}

		socket.send(new DatagramPacket(buffer, offset, address));
	}

	/**
	 * Puts a string into the buffer by first writing the size of the string as
	 * an int, followed by the bytes of the string, padded if necessary to a
	 * multiple of 4. If the String value exceeds the available length of the
	 * buffer, this will trim the String to fit and add an ellipsis at the end.
	 */
	protected void xdr_string(String s) {
		byte[] bytes = s.getBytes();
		int len = bytes.length;
		// The available buffer is equal to the total buffer size, minus our
		// current offset, minus 4 bytes
		// to write the xdr integer length value
		int availableBuffer = BUFFER_SIZE - offset - 4;
		if (len < availableBuffer) {
			xdr_int(len);
			System.arraycopy(bytes, 0, buffer, offset, len);
			offset += len;
		} else {
			xdr_int(availableBuffer);
			bytes[availableBuffer - 1] = '.';
			bytes[availableBuffer - 2] = '.';
			bytes[availableBuffer - 3] = '.';
			System.arraycopy(bytes, 0, buffer, offset, availableBuffer);
			offset += availableBuffer;
		}
		pad();
	}

	/**
	 * Pads the buffer with zero bytes up to the nearest multiple of 4.
	 */
	private void pad() {
		int newOffset = ((offset + 3) / 4) * 4;
		while (offset < newOffset) {
			buffer[offset++] = 0;
		}
	}

	/**
	 * Puts an integer into the buffer as 4 bytes, big-endian.
	 */
	protected void xdr_int(int i) {
		buffer[offset++] = (byte) ((i >> 24) & 0xff);
		buffer[offset++] = (byte) ((i >> 16) & 0xff);
		buffer[offset++] = (byte) ((i >> 8) & 0xff);
		buffer[offset++] = (byte) (i & 0xff);
	}

	private class MetricMetaData {

		private String hostName;
		private String metricName;
		private DataType type;

		private MetricMetaData(String hostName, String metricName, DataType type) {
			this.hostName = hostName;
			this.metricName = metricName;
			this.type = type;
		}

		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (o.getClass() != this.getClass()) {
				return false;
			}

			if (!(o instanceof MetricMetaData)) {
				return false;
			}

			MetricMetaData other = (MetricMetaData) o;

			return new EqualsBuilder().append(this.hostName, other.hostName).append(this.metricName, other.metricName).append(this.type, other.type)
					.isEquals();
		}

		public int hashCode() {
			return new HashCodeBuilder(41, 97).append(hostName).append(metricName).append(type).toHashCode();
		}

		public String toString() {
			return "MetricMetaData{" + "hostName='" + hostName + '\'' + ", metricName='" + metricName + '\'' + ", type=" + type + '}';
		}
	}

}
