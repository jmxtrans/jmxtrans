package com.googlecode.jmxtrans.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.OutputWriter;
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
     * Either invokes the queries multithreaded (max threads == server.getMultiThreaded())
     * or invokes them one at a time.
     */
    public static void processQueriesForServer(final MBeanServerConnection mbeanServer, Server server) throws Exception {
        
        if (server.isQueriesMultiThreaded()) {
            ExecutorService service = null;
            try {
                service = Executors.newFixedThreadPool(server.getNumQueryThreads());
                List<Callable<Object>> threads = new ArrayList<Callable<Object>>(server.getQueries().size());
                for (Query query : server.getQueries()) {
                    query.setServer(server);
                    ProcessQueryThread pqt = new ProcessQueryThread(mbeanServer, query);
                    threads.add(Executors.callable(pqt));
                }
                service.invokeAll(threads);
            } finally {
                service.shutdown();
            }
        } else {
            for (Query query : server.getQueries()) {
                query.setServer(server);
                processQuery(mbeanServer, query);
            }
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
                processQuery(mbeanServer, query);
            } catch (Exception e) {
                log.error("Error executing query", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void processQuery(MBeanServerConnection mbeanServer, Query query) throws Exception {
        
        List<Result> resList = new ArrayList<Result>();

        ObjectName oName = new ObjectName(query.getObj());

        Set<ObjectName> queryNames = mbeanServer.queryNames(oName, null);
        for (ObjectName queryName : queryNames) {
            MBeanInfo info = mbeanServer.getMBeanInfo(queryName);
            ObjectInstance oi = mbeanServer.getObjectInstance(queryName);

            List<String> queryAttributes = query.getAttr();
            if (queryAttributes == null || queryAttributes.size() == 0) {
                MBeanAttributeInfo[] attrs = info.getAttributes();
                for (MBeanAttributeInfo attrInfo : attrs) {
                    query.addAttr(attrInfo.getName());
                }
            }
            AttributeList al = mbeanServer.getAttributes(queryName, query.getAttr().toArray(new String[query.getAttr().size()]));
            for (Attribute attribute : al.asList()) {
                getResult(resList, info, oi, (Attribute)attribute, query);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Executed query: " + query);
        }
        
        query.setResults(resList);

        // Now run the filters.
        runFiltersForQuery(query);

        if (log.isDebugEnabled()) {
            log.debug("Finished running filters: " + query);
        }
    }

    /**
     * Populates the Result objects. This is a recursive function. Query contains the
     * keys that we want to get the values of.
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
                r.addValue(key, value.toString());
            } else if (value instanceof CompositeDataSupport) {
                // now recursively go through everything.
                CompositeDataSupport cds2 = (CompositeDataSupport) value;
                getResult(resList, info, oi, attributeName, (CompositeDataSupport)cds2, query);
                return; // because we don't want to add to the list yet.
            } else {
                r.addValue(key, value.toString());
            }
        }
        resList.add(r);
    }

    /** */
    private static void processTabularDataSupport(List<Result> resList, MBeanInfo info, ObjectInstance oi, Result r, String attributeName, TabularDataSupport tds, Query query) {
        Set<Entry<Object,Object>> entries = tds.entrySet();
        for (Entry<Object, Object> entry : entries) {
            Object entryKeys = entry.getKey();
            if (entryKeys instanceof List) {
                // ie: attributeName=LastGcInfo.Par Survivor Space
                // i haven't seen this be smaller or larger than List<1>, but might as well loop it.
                StringBuilder sb = new StringBuilder();
                for (Object entryKey : (List<?>)entryKeys) {
                    sb.append(".");
                    sb.append(entryKey);
                }
                String attributeName2 = sb.toString();
                Object entryValue = entry.getValue();
                if (entryValue instanceof CompositeDataSupport) {
                    getResult(resList, info, oi, attributeName + attributeName2, (CompositeDataSupport)entryValue, query);
                } else {
                    throw new RuntimeException("!!!!!!!!!! Please file a bug: http://code.google.com/p/jmxtrans/issues/entry entryValue is: " + entryValue.getClass().getCanonicalName());
                }
            } else {
                throw new RuntimeException("!!!!!!!!!! Please file a bug: http://code.google.com/p/jmxtrans/issues/entry entryKeys is: " + entryKeys.getClass().getCanonicalName());
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
            if (value instanceof CompositeDataSupport) {
                getResult(resList, info, oi, attribute.getName(), (CompositeData) value, query);
            } else if (value instanceof CompositeData[]) {
                for (CompositeData cd : (CompositeData[])value) {
                    getResult(resList, info, oi, attribute.getName(), cd, query);
                }
            } else if (value instanceof ObjectName[]) {
                Result r = getNewResultObject(info, oi, attribute.getName(), query);
                for (ObjectName obj : (ObjectName[])value) {
                    r.addValue(obj.getCanonicalName(), obj.getKeyPropertyListString());
                }
                resList.add(r);
            } else if (value.getClass().isArray()) {
                // OMFG: this is nutty. some of the items in the array can be primitive! great interview question!
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
                r.addValue(attribute.getName(), value.toString());
                resList.add(r);
            }
        }
    }

    /** */
    private static void runFiltersForQuery(Query query) throws Exception {
        List<OutputWriter> filters = query.getOutputWriters();
        if (filters != null) {
            for (OutputWriter filter : filters) {
                filter.doWrite(query);
            }
        }
    }

    /**
     * Generates the proper username/password environment for JMX connections.
     */
    public static Map<String, String[]> getEnvironment(Server server) {
        Map<String, String[]> environment = new HashMap<String, String[]>();
        String username = server.getUsername();
        String password = server.getPassword();
        if (username != null && password != null) {
            String[] credentials = new String[2];
            credentials[0] = username;
            credentials[1] = password;
            environment.put(JMXConnector.CREDENTIALS, credentials);
        }
        return environment;
    }

    /**
     * Either invokes the servers multithreaded (max threads == jmxProcess.getMultiThreaded())
     * or invokes them one at a time.
     */
    public static void execute(JmxProcess process) throws Exception {
        
        if (process.isServersMultiThreaded()) {
            ExecutorService service = null;
            try {
                service = Executors.newFixedThreadPool(process.getNumMultiThreadedServers());
                List<Callable<Object>> threads = new ArrayList<Callable<Object>>(process.getServers().size());
                for (Server server : process.getServers()) {
                    ProcessServerThread pqt = new ProcessServerThread(server);
                    threads.add(Executors.callable(pqt));
                }
                service.invokeAll(threads);
            } finally {
                service.shutdown();
            }
        } else {
            for (Server server : process.getServers()) {
                processServer(server);
            }
        }
    }

    /**
     * Executes either a getAttribute or getAttributes query.
     */
    public static class ProcessServerThread implements Runnable {
        private Server server;

        public ProcessServerThread(Server server) {
            this.server = server;
        }
        
        public void run() {
            try {
                processServer(server);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Helper method for connecting to a Server. You need to close the resulting connection.
     */
    public static JMXConnector getServerConnection(Server server) throws Exception {
        JMXServiceURL url = new JMXServiceURL(server.getUrl());
        return JMXConnectorFactory.connect(url, getEnvironment(server));
    }

    /**
     * Does the work for processing a Server object.
     */
    public static void processServer(Server server) throws Exception {
        JMXConnector conn = null;
        try {
            conn = getServerConnection(server);
            MBeanServerConnection mbeanServer = conn.getMBeanServerConnection();

            JmxUtils.processQueriesForServer(mbeanServer, server);
        } catch (IOException e) {
            log.error("Problem processing queries for server: " + server.getHost() + ":" + server.getPort(), e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    /**
     * Utility function good for testing things. Prints out the json
     * tree of the JmxProcess.
     */
    public static void printJson(JmxProcess process) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().set(Feature.WRITE_NULL_MAP_VALUES, false);
        System.out.println(mapper.writeValueAsString(process));
    }

    /**
     * Utility function good for testing things. Prints out the json
     * tree of the JmxProcess.
     */
    public static void prettyPrintJson(JmxProcess process) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().set(Feature.WRITE_NULL_MAP_VALUES, false);
        ObjectWriter writer = mapper.defaultPrettyPrintingWriter();
        System.out.println(writer.writeValueAsString(process));
    }

    /**
     * Uses jackson to load json configuration from a File into a full object
     * tree representation of that json.
     */
    public static JmxProcess getJmxProcess(File file) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JmxProcess jmx = mapper.readValue(file, JmxProcess.class);
        jmx.setName(file.getName());
        return jmx;
    }

    /**
     * Useful for figuring out if an Object is a number.
     */
    public static boolean isNumeric(Object value) {
        return ((value instanceof String && StringUtils.isNumeric((String)value)) || 
                value instanceof Number || value instanceof Integer || 
                value instanceof Long || value instanceof Double || value instanceof Float);
    }
}
