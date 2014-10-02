package com.googlecode.jmxtrans.example;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.jmx.IdentityValueTransformer;
import com.googlecode.jmxtrans.jmx.JmxQueryProcessor;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyMap;

/**
 * Walks a JMX tree and prints out all of the unique typenames and their
 * attributes.
 * 
 * This is a good test of the core engine of JmxTrans to ensure that it covers
 * all cases.
 * 
 * @author jon
 */
public class TreeWalker3 {

	private static final Logger log = LoggerFactory.getLogger(TreeWalker3.class);

	/** */
	public static void main(String[] args) throws Exception {
		Server server = Server.builder().setHost("w2").setPort("1105").build();

		JMXConnector conn = null;
		try {
			conn = server.getServerConnection();
			MBeanServerConnection mbeanServer = conn.getMBeanServerConnection();

			TreeWalker3 tw = new TreeWalker3();
			tw.walkTree(mbeanServer);
		} catch (IOException e) {
			log.error("Problem processing queries for server: " + server.getHost() + ":" + server.getPort(), e);
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	/** */
	public void walkTree(MBeanServerConnection connection) throws Exception {

		// key here is null, null returns everything!
		Set<ObjectName> mbeans = connection.queryNames(null, null);

		Map<String, String> output = newHashMap();

		for (ObjectName name : mbeans) {
			MBeanInfo info = connection.getMBeanInfo(name);
			MBeanAttributeInfo[] attrs = info.getAttributes();

			Query.Builder queryBuilder = Query.builder()
					.setObj(name.getCanonicalName());
			ResultCapture resultCapture = new ResultCapture();
			queryBuilder.addOutputWriter(resultCapture);

			for (MBeanAttributeInfo attrInfo : attrs) {
				queryBuilder.addAttr(attrInfo.getName());
			}

			Query query = queryBuilder.build();

			try {
				new JmxQueryProcessor(new IdentityValueTransformer()).processQuery(connection, null, query);
			} catch (AttributeNotFoundException anfe) {
				log.error("Error", anfe);
			}

			for (Result result : resultCapture.results) {
				output.put(result.getTypeName(), query.getAttr().toString());
			}
		}

		for (Entry<String, String> entry : output.entrySet()) {
			log.debug(entry.getKey());
			log.debug(entry.getValue());
			log.debug("-----------------------------------------");
		}
	}

	private static final class ResultCapture implements OutputWriter {

		private List<Result> results;

		@Override
		public void start() throws LifecycleException {}

		@Override
		public void stop() throws LifecycleException {}

		@Override
		public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
			this.results = results;
		}

		@Override
		public Map<String, Object> getSettings() {
			return emptyMap();
		}

		@Override
		public void setSettings(Map<String, Object> settings) {}

		@Override
		public void validateSetup(Server server, Query query) throws ValidationException {}

	}
}
