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
package com.googlecode.jmxtrans.jmx;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.List;

public class JmxQueryProcessor {
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Responsible for processing individual Queries.
	 */
	public void processQuery(MBeanServerConnection mbeanServer, Server server, Query query) throws Exception {
		ObjectName oName = new ObjectName(query.getObj());
		
		for (ObjectName queryName : mbeanServer.queryNames(oName, null)) {
			ImmutableList<Result> results = fetchResults(mbeanServer, query, queryName);
			runOutputWritersForQuery(server, query, results);
		}
	}

	private ImmutableList<Result> fetchResults(MBeanServerConnection mbeanServer, Query query, ObjectName queryName) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		MBeanInfo info = mbeanServer.getMBeanInfo(queryName);
		ObjectInstance oi = mbeanServer.getObjectInstance(queryName);

		List<String> attributes;
		if (query.getAttr().isEmpty()) {
			attributes = new ArrayList<String>();
			for (MBeanAttributeInfo attrInfo : info.getAttributes()) {
				attributes.add(attrInfo.getName());
			}
		} else {
			attributes = query.getAttr();
		}

		ImmutableList<Result> results = ImmutableList.of();
		try {
			if (attributes.size() > 0) {
				log.debug("Executing queryName [{}] from query [{}]", queryName.getCanonicalName(), query);

				AttributeList al = mbeanServer.getAttributes(queryName, attributes.toArray(new String[attributes.size()]));

				results = new JmxResultProcessor(query, oi, al.asList(), info.getClassName(), queryName.getDomain()).getResults();
			}
		} catch (UnmarshalException ue) {
			if ((ue.getCause() != null) && (ue.getCause() instanceof ClassNotFoundException)) {
				log.debug("Bad unmarshall, continuing. This is probably ok and due to something like this: "
						+ "http://ehcache.org/xref/net/sf/ehcache/distribution/RMICacheManagerPeerListener.html#52", ue.getMessage());
			}
		}
		return results;
	}

	private void runOutputWritersForQuery(Server server, Query query, ImmutableList<Result> results) throws Exception {
		for (OutputWriter writer : query.getOutputWriters()) {
			writer.doWrite(server, query, results);
		}
		log.debug("Finished running outputWriters for query: {}", query);
	}

}
