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
package com.googlecode.jmxtrans.jobs;

import com.googlecode.jmxtrans.connections.JMXConnectionParams;
import com.googlecode.jmxtrans.jmx.JmxUtils;
import com.googlecode.jmxtrans.model.Server;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

/**
 * This is a quartz job that is responsible for executing a Server object on a
 * cron schedule that is defined within the Server object.
 *
 * @author jon
 */
public class ServerJob implements Job {

	private static final Logger log = LoggerFactory.getLogger(ServerJob.class);

	private final GenericKeyedObjectPool<JMXConnectionParams, JMXConnector> jmxPool;

	private final JmxUtils jmxUtils;

	@Inject
	public ServerJob(GenericKeyedObjectPool<JMXConnectionParams, JMXConnector> jmxPool, JmxUtils jmxUtils) {
		this.jmxPool = jmxPool;
		this.jmxUtils = jmxUtils;
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap map = context.getMergedJobDataMap();
		Server server = (Server) map.get(Server.class.getName());

		log.debug("+++++ Started server job: {}", server);

		JMXConnector conn = null;
		JMXConnectionParams connectionParams = null;

		try {
			JMXServiceURL jmxUrl = server.getJmxServiceURL();

			connectionParams = new JMXConnectionParams(jmxUrl, server.getEnvironment());
			if (!server.isLocal()) {
				conn = jmxPool.borrowObject(connectionParams);
			}
			jmxUtils.processServer(server, conn);
		} catch (Exception e) {
			log.error("Error in job for server: " + server, e);
			throw new JobExecutionException(e);
		} finally {
			try {
				jmxPool.returnObject(connectionParams, conn);
			} catch (Exception ex) {
				log.error("Error returning object to pool for server: " + server);
			}
		}

		log.debug("+++++ Finished server job: {}", server);
	}
}
