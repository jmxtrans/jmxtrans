/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
