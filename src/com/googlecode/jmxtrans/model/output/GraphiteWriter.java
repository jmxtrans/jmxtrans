package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
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

	private static final String DEFAULT_ROOT_PREFIX = "servers";

	private String rootPrefix = "servers";

	private GenericKeyedObjectPool<InetSocketAddress, Socket> pool;

	private InetSocketAddress address;

	@JsonCreator
	public GraphiteWriter(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("settings") Map<String, Object> settings) {
		super(typeNames, debugEnabled, settings);
	}

	public void validateSetup(Server server, Query query) throws ValidationException {
		String host = Settings.getStringSetting(this.getSettings(), HOST, null);
		Integer port = Settings.getIntegerSetting(this.getSettings(), PORT, null);

		if (host == null || port == null) {
			throw new ValidationException("Host and port can't be null", query);
		}

		rootPrefix = Settings.getStringSetting(this.getSettings(), ROOT_PREFIX, DEFAULT_ROOT_PREFIX);

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
