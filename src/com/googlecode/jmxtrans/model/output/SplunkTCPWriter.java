package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
 * This output writer sends data to a host/port combination in the splunk format.
 *
 * it is basically a copy/past of jon's graphite writer with small changes
 * 
 * @author oded
 */
public class SplunkTCPWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(SplunkTCPWriter.class);

	private String host;
	private Integer port;

	private KeyedObjectPool pool;
	private ManagedObject mbean;
	private InetSocketAddress address;

	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 */
	public SplunkTCPWriter() { }

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

	@Override
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

		this.address = new InetSocketAddress(host, port);
	}

	@Override
	public void doWrite(Query query) throws Exception {

		Socket socket = (Socket) pool.borrowObject(this.address);
		try {
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

			List<Result> results = query.getResults();
			
			if (results.size() > 0) {
				StringBuilder sb = new StringBuilder();
				Result r = results.get(0);
				DateFormat fdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
				sb.append(fdate.format(new Date(r.getEpoch())));
				sb.append(" ");
				sb.append("jmxhost=");
				sb.append(query.getServer().getHost());
				sb.append(" ");
				sb.append(r.getTypeName().replace(",", " "));
				for (Result result : results) {
					Map<String, Object> resultValues = result.getValues();
					if (resultValues != null) {
						for (Entry<String, Object> values : resultValues.entrySet()) {
							if (JmxUtils.isNumeric(values.getValue())) {
								sb.append(" ");
								sb.append(result.getAttributeName());
								sb.append("=");
								sb.append(values.getValue().toString());	
							}
						}
					}
				}
				sb.append("\r\n");
				String s = sb.toString();
				if (isDebugEnabled()) {
					log.debug("Splunk TCP writer Message: " + s.trim());
				}
				writer.write(s);
				writer.flush();
			}
		}
		catch (Exception e) {
			log.error(e.getMessage());
		}
		finally {
			pool.returnObject(address, socket);
		}
	}

}
