package com.googlecode.jmxtrans.jobs;

import javax.management.remote.JMXConnector;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxConnectionFactory;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This is a quartz job that is responsible for executing a Server object
 * on a cron schedule that is defined within the Server object.
 *
 * @author jon
 */
public class ServerJob implements Job {
	private static final Logger log = LoggerFactory.getLogger(ServerJob.class);

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap map = context.getMergedJobDataMap();
		Server server = (Server) map.get(Server.class.getName());
		JmxConnectionFactory conn = (JmxConnectionFactory) map.get(Server.JMX_CONNECTION_FACTORY_POOL);

		if (log.isDebugEnabled()) {
			log.debug("+++++ Started server job: " + server);
		}

		try {
			JmxUtils.processServer(server, (JMXConnector)conn.makeObject(server));
		} catch (Exception e) {
			log.error("Error", e);
			throw new JobExecutionException(e);
		}

		if (log.isDebugEnabled()) {
			log.debug("+++++ Finished server job: " + server);
		}
	}
}
