package com.googlecode.jmxtrans.model.output;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * This output writer sends data to a host/port combination in the StatsD
 * format.
 *
 * @author neilh
 */
public class StatsDWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(StatsDWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";

	private String host;
	private Integer port;
	private String rootPrefix = "servers";
	private InetAddress ipAddress;

	private KeyedObjectPool pool;
	private ManagedObject mbean;

	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 */
	public StatsDWriter() {
	}

	@Override
	public void start() throws LifecycleException {
		try {
			this.pool = JmxUtils.getObjectPool(new SocketFactory());
			this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool) pool, Server.SOCKET_FACTORY_POOL);
			JmxUtils.registerJMX(this.mbean);
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	@Override
	public void stop() throws LifecycleException {
		try {
			if (this.mbean != null) {
				JmxUtils.unregisterJMX(this.mbean);
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

		try {
			ipAddress = InetAddress.getByName(host);
		} catch (UnknownHostException uhe) {
			log.debug("Exception: " + uhe.getMessage());
		}
	}

	public void doWrite(Query query) throws Exception {

		List<String> typeNames = this.getTypeNames();

		DatagramSocket socket = new DatagramSocket();
		try {
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

							sb.append(":");
							sb.append(values.getValue().toString());
							sb.append("|");
							sb.append("c\n");

							String line = sb.toString();
							byte[] sendData = sb.toString().trim().getBytes();

							if (isDebugEnabled()) {
								log.debug("StatsD Message: " + line.trim());
							}

							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
							socket.send(sendPacket);
						}
					}
				}
			}
		} finally {
			if (socket != null && ! socket.isClosed()) {
				socket.close();
			}
		}
	}
}
