package com.googlecode.jmxtrans.model.output;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.ValidationException;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.DatagramSocketFactory;
import com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPool;

/**
 * This output writer sends data to a host/port combination
 * in the Metricsd format via udp .
 *
 * @see <a href="https://github.com/mojodna/metricsd">https://github.com/mojodna/metricsd</a>
 *
 * @author Yongjun Rong
 */
public class MetricsdWriter extends GraphiteWriter {

    private static final Logger log = LoggerFactory.getLogger(MetricsdWriter.class);
  
    public MetricsdWriter() {
        super();
    }

	
	/**
	 * Using UDP DatagramSocket
	 */
   	@Override
	public void start() throws LifecycleException {
		try {
			this.pool = JmxUtils.getObjectPool(new DatagramSocketFactory());
			this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool)pool, Server.DATAGRAM_SOCKET_FACTORY_POOL);
			JmxUtils.registerJMX(this.mbean);
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

    /** */
	@Override
    public void doWrite(Query query) throws Exception {
		String metricsType = query.getMetricsType();
		DatagramSocket socket = (DatagramSocket) this.pool.borrowObject(address);
		try {
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
							String keyStr=JmxUtils.getStatsdKeyString(query, result, values, typeNames, rootPrefix);			
							sb.append(keyStr).append(":");
							sb.append(values.getValue());
							if ("histogram".equalsIgnoreCase(metricsType)) {
								sb.append("|h");
							} else if ("count".equalsIgnoreCase(metricsType)) {
								sb.append("|c");
							} else if ("time".equalsIgnoreCase(metricsType)) {
								sb.append("|ms");
							} else if ("meter".equalsIgnoreCase(metricsType)) {
								sb.append("|m");
							} else {
								sb.append("|g");
							}					
							String line = sb.toString();
							log.debug("Metricsd Message: " + line.trim());
							byte [ ] buffer = line.getBytes();
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
							socket.send(packet);
						}
					}
				}
			}
		} finally {
			this.pool.returnObject(address, socket);
		}
	}
}
