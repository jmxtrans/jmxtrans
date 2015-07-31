package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Set;

/**
 * Walks a JMX tree and prints out all of the attribute values.
 * 
 * Does not use the JmxTrans api. This is just more of a proof of concept and
 * shows how utterly broken and stupid the jmx api is. The fact that you have to
 * pass in null,null into MBeanServerConnection.queryNames() is utterly stupid.
 * 
 * This code was borrowed from the munin jmxquery plugin which I had to jad
 * decompile since I couldn't find the source to it.
 * 
 * @author jon
 */
public class TreeWalker {

	private static final Logger log = LoggerFactory.getLogger(TreeWalker.class);

	public static void main(String[] args) throws Exception {
		Server server = Server.builder().setHost("localhost").setPort("1099").build();

		JMXConnector conn = null;
		try {
			conn = server.getServerConnection();
			MBeanServerConnection mbeanServer = conn.getMBeanServerConnection();

			TreeWalker tw = new TreeWalker();
			tw.walkTree(mbeanServer);
		} catch (IOException e) {
			log.error("Problem processing queries for server: " + server.getHost() + ":" + server.getPort(), e);
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	public void walkTree(MBeanServerConnection connection) throws Exception {

		// key here is null, null returns everything!
		Set<ObjectName> mbeans = connection.queryNames(null, null);
		for (ObjectName name : mbeans) {
			MBeanInfo info = connection.getMBeanInfo(name);
			MBeanAttributeInfo[] attrs = info.getAttributes();
			String[] attrNames = new String[attrs.length];
			for (int i = 0; i < attrs.length; i++) {
				attrNames[i] = attrs[i].getName();
			}
			try {
				AttributeList attributes = connection.getAttributes(name, attrNames);
				for (Attribute attribute : attributes.asList()) {
					output(name.getCanonicalName() + "%" + attribute.getName(), attribute.getValue());
				}
			} catch (Exception e) {
				log.error("error getting " + name + ":" + e.getMessage(), e);
			}
		}
	}

	public void output(String name, Object attr) {
		CompositeDataSupport cds;
		if ((attr instanceof CompositeDataSupport)) {
			cds = (CompositeDataSupport) attr;
			for (String key : cds.getCompositeType().keySet()) {
				log.info(name + "." + key + ".value " + format(cds.get(key)));
			}
		} else {
			log.info(name + ".value " + format(attr));
		}
	}

	public String format(Object value) {
		if (value == null) {
			return null;
		} else if ((value instanceof String)) {
			return (String) value;
		} else if ((value instanceof Number)) {
			NumberFormat f = NumberFormat.getInstance();
			f.setMaximumFractionDigits(2);
			f.setGroupingUsed(false);
			return f.format(value);
		} else if ((value instanceof Object[])) {
			return Integer.toString(Arrays.asList((Object[]) value).size());
		}
		return value.toString();
	}
}
