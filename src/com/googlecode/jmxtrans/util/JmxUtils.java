package com.googlecode.jmxtrans.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.googlecode.jmxtrans.jmx.JmxQueryProcessor;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.scheduling.ProcessQueryThread;
import com.googlecode.jmxtrans.scheduling.ProcessServerThread;

/**
 * The worker code.
 *
 * @author jon
 */
public class JmxUtils {

	private static final Logger log = LoggerFactory.getLogger(JmxUtils.class);

	/**
	 * Does the work for processing a Server object.
	 */
	public static void processServer(Server server, JMXConnector conn) throws Exception {

		MBeanServerConnection mbeanServer;

		if (server.isLocal()) {
			mbeanServer = server.getLocalMBeanServer();
		} else {
			mbeanServer = conn.getMBeanServerConnection();
		}

		if (server.isQueriesMultiThreaded()) {
			ExecutorService service = null;
			try {
				service = Executors.newFixedThreadPool(server.getNumQueryThreads());
				if (log.isDebugEnabled()) {
					log.debug("----- Creating " + server.getQueries().size() + " query threads");
				}

				List<Callable<Object>> threads = new ArrayList<Callable<Object>>(server.getQueries().size());
				for (Query query : server.getQueries()) {
					ProcessQueryThread pqt = new ProcessQueryThread(mbeanServer, server, query);
					threads.add(Executors.callable(pqt));
				}

				service.invokeAll(threads);

			} finally {
				if (service != null) {
					shutdownAndAwaitTermination(service);
				}
			}
		} else {
			for (Query query : server.getQueries()) {
				new JmxQueryProcessor().processQuery(mbeanServer, server, query);
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
						JMXConnector conn = server.getServerConnection();
						conns.add(conn);
						service.execute(new ProcessServerThread(server, conn));
					}
				}
				service.shutdown();
			} finally {
				try {
					if (service != null) {
						service.awaitTermination(1000 * 60, TimeUnit.SECONDS);
					}
				} catch (InterruptedException ex) {
					log.error("Error shutting down execution.", ex);
				}
			}
		} else {
			for (Server server : process.getServers()) {
				if (server.getLocalMBeanServer() != null) {
					processServer(server, null);
				} else {
					JMXConnector conn = server.getServerConnection();
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

}
