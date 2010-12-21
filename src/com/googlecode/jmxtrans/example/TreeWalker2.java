package com.googlecode.jmxtrans.example;

import java.io.IOException;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * Walks a JMX tree and prints out all of the attribute values actually using the JmxTrans api.
 * 
 * This is a good test of the core engine of JmxTrans to ensure that it covers all cases.
 * 
 * @author jon
 */
public class TreeWalker2 {

    private static final Logger log = LoggerFactory.getLogger(TreeWalker2.class);

    /** */
    public static void main(String[] args) throws Exception {
        Server server = new Server("w2", "1099");

        JMXConnector conn = null;
        try {
            conn = JmxUtils.getServerConnection(server);
            MBeanServerConnection mbeanServer = conn.getMBeanServerConnection();
            
            TreeWalker2 tw = new TreeWalker2();
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
        for (ObjectName name : mbeans) {
            MBeanInfo info = connection.getMBeanInfo(name);
            MBeanAttributeInfo[] attrs = info.getAttributes();

            Query query = new Query();
            query.setObj(name.getCanonicalName());
            query.addOutputWriter(new StdOutWriter());
            
            for (MBeanAttributeInfo attrInfo : attrs) {
                query.addAttr(attrInfo.getName());
            }
            
            try {
                JmxUtils.processQuery(connection, query);
            } catch (AttributeNotFoundException anfe) {
                log.error("Error", anfe);
            }
        }
    }
}
