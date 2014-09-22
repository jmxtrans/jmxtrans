package com.googlecode.jmxtrans.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.jmx.ManagedObject;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;

/**
 * The worker code.
 *
 * @author jon
 */
public class JmxUtils {

	private static final Logger log = LoggerFactory.getLogger(JmxUtils.class);


	/**
	 * Merges two lists of servers (and their queries). Based on the equality of
	 * both sets of objects. Public for testing purposes.
	 */
	public static void mergeServerLists(List<Server> existing, List<Server> adding) {
		for (Server server : adding) {
			if (existing.contains(server)) {
				Server found = existing.get(existing.indexOf(server));

				List<Query> queries = server.getQueries();
				for (Query q : queries) {
					try {
						// no need to check for existing since this method
						// already does that
						found.addQuery(q);
					} catch (ValidationException ex) {
						// catching this exception because we don't want to stop
						// processing
						log.error("Error adding query: " + q + " to server" + server, ex);
					}
				}
			} else {
				existing.add(server);
			}
		}
	}

	/**
	 * Either invokes the queries multithreaded (max threads ==
	 * server.getMultiThreaded()) or invokes them one at a time.
	 */
	public static void processQueriesForServer(MBeanServerConnection mbeanServer, Server server) throws Exception {

		if (server.isQueriesMultiThreaded()) {
			ExecutorService service = null;
			try {
				service = Executors.newFixedThreadPool(server.getNumQueryThreads());
				if (log.isDebugEnabled()) {
					log.debug("----- Creating " + server.getQueries().size() + " query threads");
				}

				List<Callable<Object>> threads = new ArrayList<Callable<Object>>(server.getQueries().size());
				for (Query query : server.getQueries()) {
					query.setServer(server);
					ProcessQueryThread pqt = new ProcessQueryThread(mbeanServer, query);
					threads.add(Executors.callable(pqt));
				}

				service.invokeAll(threads);

			} finally {
				shutdownAndAwaitTermination(service);
			}
		} else {
			for (Query query : server.getQueries()) {
				query.setServer(server);
				processQuery(mbeanServer, query);
			}
		}
	}

	/**
	 * Copied from the Executors javadoc.
	 */
	private static void shutdownAndAwaitTermination(ExecutorService service) {
		service.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
				service.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
					log.error("Pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			service.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Executes either a getAttribute or getAttributes query.
	 */
	public static class ProcessQueryThread implements Runnable {
		private MBeanServerConnection mbeanServer;
		private Query query;

		public ProcessQueryThread(MBeanServerConnection mbeanServer, Query query) {
			this.mbeanServer = mbeanServer;
			this.query = query;
		}

		public void run() {
			try {
				processQuery(this.mbeanServer, this.query);
			} catch (Exception e) {
				log.error("Error executing query: " + query, e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Responsible for processing individual Queries.
	 */
	public static void processQuery(MBeanServerConnection mbeanServer, Query query) throws Exception {

		ObjectName oName = new ObjectName(query.getObj());

		Set<ObjectName> queryNames = mbeanServer.queryNames(oName, null);
		for (ObjectName queryName : queryNames) {

			List<Result> resList = new ArrayList<Result>();

			MBeanInfo info = mbeanServer.getMBeanInfo(queryName);
			ObjectInstance oi = mbeanServer.getObjectInstance(queryName);

			List<String> attributes;
			List<String> queryAttributes = query.getAttr();
			if ((queryAttributes == null) || (queryAttributes.size() == 0)) {
				attributes = new ArrayList<String>();
				for (MBeanAttributeInfo attrInfo : info.getAttributes()) {
					attributes.add(attrInfo.getName());
				}
			} else {
				attributes = queryAttributes;
			}

			try {
				if (attributes.size() > 0) {
					if (log.isDebugEnabled()) {
						log.debug("Executing queryName: " + queryName.getCanonicalName() + " from query: " + query);
					}

					AttributeList al = mbeanServer.getAttributes(queryName, attributes.toArray(new String[attributes.size()]));
					for (Attribute attribute : al.asList()) {
						getResult(resList, info, oi, attribute, query);
					}

					query.setResults(resList);

					// Now run the OutputWriters.
					runOutputWritersForQuery(query);

					if (log.isDebugEnabled()) {
						log.debug("Finished running outputWriters for query: " + query);
					}
				}
			} catch (UnmarshalException ue) {
				if ((ue.getCause() != null) && (ue.getCause() instanceof ClassNotFoundException)) {
					log.debug("Bad unmarshall, continuing. This is probably ok and due to something like this: "
							+ "http://ehcache.org/xref/net/sf/ehcache/distribution/RMICacheManagerPeerListener.html#52", ue.getMessage());
				}
			}
		}

	}

	/**
	 * Populates the Result objects. This is a recursive function. Query
	 * contains the keys that we want to get the values of.
	 */
	private static void getResult(List<Result> resList, MBeanInfo info, ObjectInstance oi, String attributeName, CompositeData cds, Query query) {
		CompositeType t = cds.getCompositeType();

		Result r = getNewResultObject(info, oi, attributeName, query);

		Set<String> keys = t.keySet();
		for (String key : keys) {
			Object value = cds.get(key);
			if (value instanceof TabularDataSupport) {
				TabularDataSupport tds = (TabularDataSupport) value;
				processTabularDataSupport(resList, info, oi, r, attributeName + "." + key, tds, query);
				r.addValue(key, value);
			} else if (value instanceof CompositeDataSupport) {
				// now recursively go through everything.
				CompositeDataSupport cds2 = (CompositeDataSupport) value;
				getResult(resList, info, oi, attributeName, cds2, query);
				return; // because we don't want to add to the list yet.
			} else {
				r.addValue(key, value);
			}
		}
		resList.add(r);
	}

	/** */
	private static void processTabularDataSupport(List<Result> resList, MBeanInfo info, ObjectInstance oi, Result r, String attributeName,
												  TabularDataSupport tds, Query query) {
		Set<Entry<Object, Object>> entries = tds.entrySet();
		for (Entry<Object, Object> entry : entries) {
			Object entryKeys = entry.getKey();
			if (entryKeys instanceof List) {
				// ie: attributeName=LastGcInfo.Par Survivor Space
				// i haven't seen this be smaller or larger than List<1>, but
				// might as well loop it.
				StringBuilder sb = new StringBuilder();
				for (Object entryKey : (List<?>) entryKeys) {
					sb.append(".");
					sb.append(entryKey);
				}
				String attributeName2 = sb.toString();
				Object entryValue = entry.getValue();
				if (entryValue instanceof CompositeDataSupport) {
					getResult(resList, info, oi, attributeName + attributeName2, (CompositeDataSupport) entryValue, query);
				} else {
					throw new RuntimeException("!!!!!!!!!! Please file a bug: https://github.com/jmxtrans/jmxtrans/issues entryValue is: "
							+ entryValue.getClass().getCanonicalName());
				}
			} else {
				throw new RuntimeException("!!!!!!!!!! Please file a bug: https://github.com/jmxtrans/jmxtrans/issues entryKeys is: "
						+ entryKeys.getClass().getCanonicalName());
			}
		}
	}

	/**
	 * Builds up the base Result object
	 */
	private static Result getNewResultObject(MBeanInfo info, ObjectInstance oi, String attributeName, Query query) {
		Result r = new Result(attributeName);
		r.setQuery(query);
		r.setClassName(info.getClassName());
		r.setTypeName(oi.getObjectName().getCanonicalKeyPropertyListString());
		return r;
	}

	/**
	 * Used when the object is effectively a java type
	 */
	private static void getResult(List<Result> resList, MBeanInfo info, ObjectInstance oi, Attribute attribute, Query query) {
		Object value = attribute.getValue();
		if (value != null) {
			if (value instanceof CompositeData) {
				getResult(resList, info, oi, attribute.getName(), (CompositeData) value, query);
			} else if (value instanceof CompositeData[]) {
				for (CompositeData cd : (CompositeData[]) value) {
					getResult(resList, info, oi, attribute.getName(), cd, query);
				}
			} else if (value instanceof ObjectName[]) {
				Result r = getNewResultObject(info, oi, attribute.getName(), query);
				for (ObjectName obj : (ObjectName[]) value) {
					r.addValue(obj.getCanonicalName(), obj.getKeyPropertyListString());
				}
				resList.add(r);
			} else if (value.getClass().isArray()) {
				// OMFG: this is nutty. some of the items in the array can be
				// primitive! great interview question!
				Result r = getNewResultObject(info, oi, attribute.getName(), query);
				for (int i = 0; i < Array.getLength(value); i++) {
					Object val = Array.get(value, i);
					r.addValue(attribute.getName() + "." + i, val);
				}
				resList.add(r);
			} else if (value instanceof TabularDataSupport) {
				TabularDataSupport tds = (TabularDataSupport) value;
				Result r = getNewResultObject(info, oi, attribute.getName(), query);
				processTabularDataSupport(resList, info, oi, r, attribute.getName(), tds, query);
				resList.add(r);
			} else {
				Result r = getNewResultObject(info, oi, attribute.getName(), query);
				r.addValue(attribute.getName(), value);
				resList.add(r);
			}
		}
	}

	/** */
	private static void runOutputWritersForQuery(Query query) throws Exception {
		List<OutputWriter> writers = query.getOutputWriters();
		if (writers != null) {
			for (OutputWriter writer : writers) {
				writer.doWrite(query);
			}
		}
	}

	/**
	 * Helper method for connecting to a Server. You need to close the resulting
	 * connection.
	 */
	public static JMXConnector getServerConnection(Server server) throws Exception {
		JMXServiceURL url = new JMXServiceURL(server.getUrl());

		if (server.getProtocolProviderPackages() != null && server.getProtocolProviderPackages().contains("weblogic"))
			return JMXConnectorFactory.connect(url, getWebLogicEnvironment(server));
		else
			return JMXConnectorFactory.connect(url, getEnvironment(server));

	}

	/**
	 * Generates the proper username/password environment for JMX connections.
	 */
	public static Map<String, String> getWebLogicEnvironment(Server server) {
		Map<String, String> environment = new HashMap<String, String>();
		String username = server.getUsername();
		String password = server.getPassword();
		if ((username != null) && (password != null)) {
			environment.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, server.getProtocolProviderPackages());
			environment.put(Context.SECURITY_PRINCIPAL, username);
			environment.put(Context.SECURITY_CREDENTIALS, password);
		}
		return environment;
	}

	/**
	 * Generates the proper username/password environment for JMX connections.
	 */
	public static Map<String, String[]> getEnvironment(Server server) {
		Map<String, String[]> environment = new HashMap<String, String[]>();
		String username = server.getUsername();
		String password = server.getPassword();
		if ((username != null) && (password != null)) {
			String[] credentials = new String[2];
			credentials[0] = username;
			credentials[1] = password;

			environment.put(JMXConnector.CREDENTIALS, credentials);
		}
		return environment;
	}

	/**
	 * Either invokes the servers multithreaded (max threads ==
	 * jmxProcess.getMultiThreaded()) or invokes them one at a time.
	 */
	public static void execute(JmxProcess process) throws Exception {

		List<JMXConnector> conns = new ArrayList<JMXConnector>();

		if (process.isServersMultiThreaded()) {
			ExecutorService service = null;
			try {
				service = Executors.newFixedThreadPool(process.getNumMultiThreadedServers());
				for (Server server : process.getServers()) {
					if (server.isLocal() && server.getLocalMBeanServer() != null) {
						service.execute(new ProcessServerThread(server, null));
					} else {
						JMXConnector conn = JmxUtils.getServerConnection(server);
						conns.add(conn);
						service.execute(new ProcessServerThread(server, conn));
					}
				}
				service.shutdown();
			} finally {
				try {
					service.awaitTermination(1000 * 60, TimeUnit.SECONDS);
				} catch (InterruptedException ex) {
					log.error("Error shutting down execution.", ex);
				}
			}
		} else {
			for (Server server : process.getServers()) {
				if (server.getLocalMBeanServer() != null) {
					processServer(server, null);
				} else {
					JMXConnector conn = JmxUtils.getServerConnection(server);
					conns.add(conn);
					processServer(server, conn);
				}
			}
		}

		for (JMXConnector conn : conns) {
			try {
				conn.close();
			} catch (Exception ex) {
				log.error("Error closing connection.", ex);
			}
		}
	}

	/**
	 * Executes either a getAttribute or getAttributes query.
	 */
	public static class ProcessServerThread implements Runnable {
		private Server server;
		private JMXConnector conn;

		public ProcessServerThread(Server server, JMXConnector conn) {
			this.server = server;
			this.conn = conn;
		}

		public void run() {
			try {
				processServer(this.server, this.conn);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Does the work for processing a Server object.
	 */
	public static void processServer(Server server, JMXConnector conn) throws Exception {

		MBeanServerConnection mbeanServer;

		if (server.isLocal())
			mbeanServer = server.getLocalMBeanServer();
		else
			mbeanServer = conn.getMBeanServerConnection();

		JmxUtils.processQueriesForServer(mbeanServer, server);
	}

	/**
	 * Gets the object pool. TODO: Add options to adjust the pools, this will be
	 * better performance on high load
	 *
	 * @param <T>     the generic type
	 * @param factory the factory
	 * @return the object pool
	 */
	public static <T extends KeyedPoolableObjectFactory> GenericKeyedObjectPool getObjectPool(T factory) {
		GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
		pool.setTestOnBorrow(true);
		pool.setMaxActive(-1);
		pool.setMaxIdle(-1);
		pool.setTimeBetweenEvictionRunsMillis(1000 * 60 * 5);
		pool.setMinEvictableIdleTimeMillis(1000 * 60 * 5);

		return pool;
	}

	/**
	 * Helper method which returns a default PoolMap.
	 * <p/>
	 * TODO: allow for more configuration options?
	 */
	public static Map<String, KeyedObjectPool> getDefaultPoolMap() {
		Map<String, KeyedObjectPool> poolMap = new HashMap<String, KeyedObjectPool>();

		GenericKeyedObjectPool pool = getObjectPool(new SocketFactory());
		poolMap.put(Server.SOCKET_FACTORY_POOL, pool);

		GenericKeyedObjectPool jmxPool = getObjectPool(new JmxConnectionFactory());
		poolMap.put(Server.JMX_CONNECTION_FACTORY_POOL, jmxPool);

		GenericKeyedObjectPool dsPool = getObjectPool(new DatagramSocketFactory());
		poolMap.put(Server.DATAGRAM_SOCKET_FACTORY_POOL, dsPool);

		return poolMap;
	}

	/**
	 * Register the scheduler in the local MBeanServer.
	 *
	 * @param mbean the mbean
	 * @throws Exception the exception
	 */
	public static void registerJMX(ManagedObject mbean) throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(mbean, mbean.getObjectName());
	}

	/**
	 * Unregister the scheduler from the local MBeanServer.
	 *
	 * @param mbean the mbean
	 * @throws Exception the exception
	 */
	public static void unregisterJMX(ManagedObject mbean) throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.unregisterMBean(mbean.getObjectName());
	}

	/**
	 * Gets the key string.
	 *
	 * @param query      the query
	 * @param result     the result
	 * @param values     the values
	 * @param typeNames  the type names
	 * @param rootPrefix the root prefix
	 * @return the key string
	 */
	public static String getKeyString(Query query, Result result, Entry<String, Object> values, List<String> typeNames, String rootPrefix) {
		StringBuilder sb = new StringBuilder();
		addRootPrefix(rootPrefix, sb);
		addAlias(query, sb);
		sb.append(".");
		// Allow people to use something other than the classname as the output.
		addClassName(result, sb);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyString(result, values, sb);
		return sb.toString();
	}

	/**
	 * Gets the key string, without rootPrefix nor Alias
	 *
	 * @param query     the query
	 * @param result    the result
	 * @param values    the values
	 * @param typeNames the type names
	 * @return the key string
	 */
	public static String getKeyString(Query query, Result result, Entry<String, Object> values, List<String> typeNames) {
		StringBuilder sb = new StringBuilder();
		addClassName(result, sb);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyString(result, values, sb);
		return sb.toString();
	}

	/**
	 * Gets the key string, with dot allowed
	 *
	 * @param query     the query
	 * @param result    the result
	 * @param values    the values
	 * @param typeNames the type names
	 * @return the key string
	 */
	public static String getKeyStringWithDottedKeys(Query query, Result result, Entry<String, Object> values, List<String> typeNames) {
		StringBuilder sb = new StringBuilder();
		addClassName(result, sb);
		sb.append(".");
		addTypeName(query, result, typeNames, sb);
		addKeyStringDotted(result, values, query.isAllowDottedKeys(), sb);
		return sb.toString();
	}

	private static void addRootPrefix(String rootPrefix, StringBuilder sb) {
		if (rootPrefix != null) {
			sb.append(rootPrefix);
			sb.append(".");
		}
	}

	private static void addAlias(Query query, StringBuilder sb) {
		String alias;
		if (query.getServer().getAlias() != null) {
			alias = query.getServer().getAlias();
		} else {
			alias = query.getServer().getHost() + "_" + query.getServer().getPort();
			alias = com.googlecode.jmxtrans.util.StringUtils.cleanupStr(alias);
		}
		sb.append(alias);
	}

	private static void addClassName(Result result, StringBuilder sb) {
		// Allow people to use something other than the classname as the output.
		if (result.getClassNameAlias() != null) {
			sb.append(result.getClassNameAlias());
		} else {
			sb.append(com.googlecode.jmxtrans.util.StringUtils.cleanupStr(result.getClassName()));
		}
	}

	private static void addTypeName(Query query, Result result, List<String> typeNames, StringBuilder sb) {
		String typeName = com.googlecode.jmxtrans.util.StringUtils.cleanupStr(getConcatedTypeNameValues(query, typeNames, result.getTypeName()), query.isAllowDottedKeys());
		if (typeName != null && typeName.length() > 0) {
			sb.append(typeName);
			sb.append(".");
		}
	}

	private static void addKeyString(Result result, Entry<String, Object> values, StringBuilder sb) {
		String keyStr = computeKey(result, values);
		sb.append(com.googlecode.jmxtrans.util.StringUtils.cleanupStr(keyStr));
	}

	private static void addKeyStringDotted(Result result, Entry<String, Object> values, boolean isAllowDottedKeys, StringBuilder sb) {
		String keyStr = computeKey(result, values);
		sb.append(com.googlecode.jmxtrans.util.StringUtils.cleanupStr(keyStr, isAllowDottedKeys));
	}

	private static String computeKey(Result result, Entry<String, Object> values) {
		String keyStr;
		if (values.getKey().startsWith(result.getAttributeName())) {
			keyStr = values.getKey();
		} else {
			keyStr = result.getAttributeName() + "." + values.getKey();
		}
		return keyStr;
	}

	/**
	 * Given a typeName string, get the first match from the typeNames setting.
	 * In other words, suppose you have:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the
	 * string
	 *
	 * @param typeNames   the type names
	 * @param typeNameStr the type name str
	 * @return the concated type name values
	 */
	public static String getConcatedTypeNameValues(List<String> typeNames, String typeNameStr) {
		if ((typeNames == null) || (typeNames.size() == 0)) {
			return null;
		}
		Map<String, String> typeNameValueMap = getTypeNameValueMap(typeNameStr);
		StringBuilder sb = new StringBuilder();
		for (String key : typeNames) {
			String result = typeNameValueMap.get(key);
			if (result != null) {
				sb.append(result);
				sb.append("_");
			}
		}
		return StringUtils.chomp(sb.toString(), "_");
	}

	/**
	 * Given a typeName string, create a Map with every key and value in the typeName.
	 * For example:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * Returns a Map with the following key/value pairs (excluding the quotes):
	 * <p/>
	 * "name"  =>  "PS Eden Space"
	 * "type"  =>  "MemoryPool"
	 *
	 * @param typeNameStr the type name str
	 * @return Map<String, String> of type-name-key / value pairs.
	 */
	public static Map<String, String> getTypeNameValueMap(String typeNameStr) {
		if (typeNameStr == null) {
			return java.util.Collections.EMPTY_MAP;
		}

		HashMap result = new HashMap();
		String[] tokens = typeNameStr.split(",");

		for (String oneToken : tokens) {
			if (oneToken.length() > 0) {
				String[] keyValue = splitTypeNameValue(oneToken);
				result.put(keyValue[0], keyValue[1]);
			}
		}
		return result;
	}

	/**
	 * Given a typeName string, get the first match from the typeNames setting.
	 * In other words, suppose you have:
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the
	 * string
	 *
	 * @param query     the query
	 * @param typeNames the type names
	 * @param typeName  the type name
	 * @return the concated type name values
	 */
	public static String getConcatedTypeNameValues(Query query, List<String> typeNames, String typeName) {
		Set<String> queryTypeNames = query.getTypeNames();
		if (queryTypeNames != null && queryTypeNames.size() > 0) {
			List<String> allNames = new ArrayList<String>(queryTypeNames);
			for (String name : typeNames) {
				if (!allNames.contains(name)) {
					allNames.add(name);
				}
			}
			return getConcatedTypeNameValues(allNames, typeName);
		} else {
			return getConcatedTypeNameValues(typeNames, typeName);
		}
	}

	/**
	 * Given a single type-name-key and value from a typename strig (e.g. "type=MemoryPool"), extract the key and
	 * the value and return both.
	 *
	 * @param typeNameToken - the string containing the pair.
	 * @return String[2] where String[0] = the key and String[1] = the value.  If the given string is not in the
	 * format "key=value" then String[0] = the original string and String[1] = "".
	 */
	private static String[] splitTypeNameValue(String typeNameToken) {
		String[] result;
		String[] keys = typeNameToken.split("=", 2);

		if (keys.length == 2) {
			result = keys;
		} else {
			result = new String[2];
			result[0] = keys[0];
			result[1] = "";
		}

		return result;
	}

}
