package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;

import static com.google.common.base.Charsets.UTF_8;

/**
 * This low latency and thread safe output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a href="http://graphite.wikidot.com/getting-your-data-into-graphite">Getting your data into Graphite</a>
 *
 * @author jon
 */
@NotThreadSafe
public class GraphiteWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";

	private String rootPrefix = "servers";

	private GenericKeyedObjectPool<InetSocketAddress, Socket> pool;

	private InetSocketAddress address;

	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 */
	public GraphiteWriter() { }

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

		this.address = new InetSocketAddress(host, port);
	}

	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		Socket socket = null;

		try {
			socket = pool.borrowObject(address);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

			List<String> typeNames = this.getTypeNames();

			for (Result result : results) {
				log.debug("Query result: {}", result);
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						Object value = values.getValue();
						if (NumberUtils.isNumeric(value)) {

							String line = KeyUtils.getKeyString(server, query, result, values, typeNames, rootPrefix)
									.replaceAll("[()]", "_") + " " + value.toString() + " "
									+ result.getEpoch() / 1000 + "\n";
							log.debug("Graphite Message: {}", line);
							writer.write(line);
							writer.flush();
						} else {
							log.warn("Unable to submit non-numeric value to Graphite: [{}] from result [{}]", value, result);
						}
					}
				}
			}
		} finally {
			pool.returnObject(address, socket);
		}
	}

	@Inject
	public void setPool(GenericKeyedObjectPool<InetSocketAddress, Socket> pool) {
		this.pool = pool;
	}

}
