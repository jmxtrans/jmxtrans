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

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.jmx.JmxQueryProcessor;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Walks a JMX tree and prints out all of the attribute values actually using
 * the JmxTrans api.
 * 
 * This is a good test of the core engine of JmxTrans to ensure that it covers
 * all cases.
 * 
 * @author jon
 */
public class TreeWalker2 {

	private static final Logger log = LoggerFactory.getLogger(TreeWalker2.class);

	/** */
	public static void main(String[] args) throws Exception {
		Server server = Server.builder().setHost("localhost").setPort("1099").build();

		JMXConnector conn = null;
		try {
			conn = server.getServerConnection();
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

			Query.Builder query = Query.builder()
					.setObj(name.getCanonicalName())
					.addOutputWriter(new StdOutWriter(ImmutableList.<String>of(), false, false, Collections.<String, Object>emptyMap()));

			for (MBeanAttributeInfo attrInfo : attrs) {
				query.addAttr(attrInfo.getName());
			}

			try {
				new JmxQueryProcessor().processQuery(connection, null, query.build());
			} catch (AttributeNotFoundException anfe) {
				log.error("Error", anfe);
			}
		}
	}
}
