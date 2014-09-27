package com.googlecode.jmxtrans.jobs;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;

import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.pool.PoolUtils;
import com.googlecode.jmxtrans.util.JmxUtils;

/**
 * This is a quartz job that is responsible for executing a Server object on a
 * cron schedule that is defined within the Server object.
 *
 * @author jon
 */
public class ServerJob implements Job {
	private static final Logger log = LoggerFactory.getLogger(ServerJob.class);

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap map = context.getMergedJobDataMap();
		Server server = (Server) map.get(Server.class.getName());
		GenericKeyedObjectPool pool = (GenericKeyedObjectPool) map.get(PoolUtils.JMX_CONNECTION_FACTORY_POOL);

		log.debug("+++++ Started server job: {}", server);

		JMXConnector conn = null;
		try {
			if (!server.isLocal()) {
				conn = (JMXConnector) pool.borrowObject(server);
			}
			JmxUtils.processServer(server, conn);
		} catch (Exception e) {
			log.error("Error in job for server: " + server, e);
			throw new JobExecutionException(e);
		} finally {
			try {
				pool.returnObject(server, conn);
			} catch (Exception ex) {
				log.error("Error returning object to pool for server: " + server);
			}
		}

		log.debug("+++++ Finished server job: {}", server);
	}
}
