package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
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
 * This output writer sends data to a host/port combination in the Graphite
 * format.
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

	private KeyedObjectPool pool;
	private ManagedObject mbean;
	private InetSocketAddress address;

	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 */
	public GraphiteWriter() { }

	@Override
	public void start() throws LifecycleException {
		try {
			this.pool = JmxUtils.getObjectPool(new SocketFactory());
			this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool)pool, Server.SOCKET_FACTORY_POOL);
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

		this.address = new InetSocketAddress(host, port);
	}

	/** */
	public void doWrite(Query query) throws Exception {

		Socket socket = (Socket) pool.borrowObject(address);
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
}
