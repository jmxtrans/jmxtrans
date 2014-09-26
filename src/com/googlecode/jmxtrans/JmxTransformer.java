package com.googlecode.jmxtrans;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.jmxtrans.cli.CliArgumentParser;
import com.googlecode.jmxtrans.jmx.ManagedGenericKeyedObjectPool;
import com.googlecode.jmxtrans.jmx.ManagedJmxTransformerProcess;
import com.googlecode.jmxtrans.jobs.ServerJob;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.JsonUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import com.googlecode.jmxtrans.util.WatchDir;
import com.googlecode.jmxtrans.util.WatchedCallback;

import static com.googlecode.jmxtrans.util.JmxUtils.mergeServerLists;

/**
 * Main() class that takes an argument which is the directory to look in for
 * files which contain json data that defines queries to run against JMX
 * servers.
 *
 * @author jon
 */
public class JmxTransformer implements WatchedCallback {

	private static final Logger log = LoggerFactory.getLogger(JmxTransformer.class);

	private Scheduler serverScheduler;

	private WatchDir watcher;

	private JmxTransConfiguration configuration;

	/**
	 * Pools. TODO : Move to a PoolUtils or PoolRegistry so others can use it
	 */
	private Map<String, KeyedObjectPool> poolMap;
	private final Map<String, ManagedGenericKeyedObjectPool> poolMBeans = new ConcurrentHashMap<String, ManagedGenericKeyedObjectPool>();

	private List<Server> masterServersList = new ArrayList<Server>();

	/**
	 * The shutdown hook.
	 */
	private Thread shutdownHook = new ShutdownHook();

	private volatile boolean isRunning = false;

	/** */
	public static void main(String[] args) throws Exception {
		JmxTransformer transformer = new JmxTransformer();

		// Start the process
		transformer.doMain(args);
	}

	/**
	 * The real main method.
	 */
	private void doMain(String[] args) throws Exception {
		this.configuration = new CliArgumentParser().parseOptions(args);

		if (configuration.isHelp()) {
			return;
		}

		ManagedJmxTransformerProcess mbean = new ManagedJmxTransformerProcess(this, configuration);
		JmxUtils.registerJMX(mbean);

		// Start the process
		this.start();

		while (true) {
			// look for some terminator
			// attempt to read off queue
			// process message
			// TODO : Make something here, maybe watch for files?
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				break;
			}
		}

		JmxUtils.unregisterJMX(mbean);
	}

	/**
	 * Start.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	public synchronized void start() throws LifecycleException {
		if (isRunning) {
			throw new LifecycleException("Process already started");
		} else {
			log.info("Starting Jmxtrans on : " + this.configuration.getJsonDirOrFile().toString());
			try {
				this.startupScheduler();

				this.startupWatchdir();

				this.setupObjectPooling();

				this.startupSystem();

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new LifecycleException(e);
			}

			// Ensure resources are free
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			isRunning = true;
		}
	}

	/**
	 * Stop.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	public synchronized void stop() throws LifecycleException {
		if (!isRunning) {
			throw new LifecycleException("Process already stoped");
		} else {
			try {
				log.info("Stopping Jmxtrans");

				// Remove hook to not call twice
				if (shutdownHook != null) {
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
				}

				this.stopServices();
				isRunning = false;
			} catch (LifecycleException e) {
				log.error(e.getMessage(), e);
				throw new LifecycleException(e);
			}
		}
	}

	/**
	 * Stop services.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	// There is a sleep to work around a Quartz issue. The issue is marked to be
	// fixed, but will require further analysis. This should not be reported by
	// Findbugs, but as a more complex issue.
	@SuppressFBWarnings(value = "SWL_SLEEP_WITH_LOCK_HELD", justification = "Workaround for Quartz issue")
	private synchronized void stopServices() throws LifecycleException {
		try {
			// Shutdown the scheduler
			if (this.serverScheduler != null) {
				this.serverScheduler.shutdown(true);
				log.debug("Shutdown server scheduler");
				try {
					// FIXME: Quartz issue, need to sleep
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
				}
				this.serverScheduler = null;
			}

			// Shutdown the file watch service
			if (this.watcher != null) {
				this.watcher.stopService();
				this.watcher = null;
				log.debug("Shutdown watch service");
			}

			for (String key : poolMap.keySet()) {
				JmxUtils.unregisterJMX(poolMBeans.get(key));
			}
			this.poolMBeans.clear();

			// Shutdown the pools
			for (Entry<String, KeyedObjectPool> entry : this.poolMap.entrySet()) {
				try {
					entry.getValue().close();
					log.debug("Closed object pool factory: " + entry.getKey());
				} catch (Exception ex) {
					log.error("Error closing object pool factory: " + entry.getKey());
				}
			}
			this.poolMap = null;

			// Shutdown the outputwriters
			this.stopWriterAndClearMasterServerList();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new LifecycleException(e);
		}
	}

	/**
	 * Shut down the output writers and clear the master server list
	 * Used both during shutdown and when re-reading config files
	 */
	private void stopWriterAndClearMasterServerList() {
		for (Server server : this.masterServersList) {
			for (Query query : server.getQueries()) {
				for (OutputWriter writer : query.getOutputWriters()) {
					try {
						writer.stop();
						log.debug("Stopped writer: " + writer.getClass().getSimpleName() + " for query: " + query);
					} catch (LifecycleException ex) {
						log.error("Error stopping writer: " + writer.getClass().getSimpleName() + " for query: " + query);
					}
				}
			}
		}
		this.masterServersList.clear();
	}

	/**
	 * Startup the watchdir service.
	 */
	private void startupWatchdir() throws Exception {
		File dirToWatch;
		if (this.configuration.getJsonDirOrFile().isFile()) {
			dirToWatch = new File(FilenameUtils.getFullPath(this.configuration.getJsonDirOrFile().getAbsolutePath()));
		} else {
			dirToWatch = this.configuration.getJsonDirOrFile();
		}

		// start the watcher
		this.watcher = new WatchDir(dirToWatch, this);
		this.watcher.start();
	}

	/**
	 * start the server scheduler which loops over all the Server jobs
	 */
	private void startupScheduler() throws Exception {
		StdSchedulerFactory serverSchedFact = new StdSchedulerFactory();
		Closer closer = Closer.create();
		try {
			InputStream stream;
			if (configuration.getQuartPropertiesFile() == null) {
				stream = closer.register(JmxTransformer.class.getResourceAsStream("/quartz.server.properties"));
			} else {
				stream = closer.register(new FileInputStream(configuration.getQuartPropertiesFile()));
			}
			serverSchedFact.initialize(stream);
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
		this.serverScheduler = serverSchedFact.getScheduler();
		this.serverScheduler.start();
	}

	/**
	 * Handy method which runs the JmxProcess
	 */
	public void executeStandalone(JmxProcess process) throws Exception {
		this.masterServersList = process.getServers();

		this.startupScheduler();
		this.setupObjectPooling();

		this.processServersIntoJobs(this.serverScheduler);

		// Sleep for 10 seconds to wait for jobs to complete.
		// There should be a better way, but it seems that way isn't working
		// right now.
		Thread.sleep(10 * 1000);
	}

	/**
	 * Processes files into Server objects and then processesServers into jobs
	 */
	private void startupSystem() throws LifecycleException {
		// process all the json files into Server objects
		this.processFilesIntoServers(this.getJsonFiles());

		// process the servers into jobs
		this.processServersIntoJobs(this.serverScheduler);
	}

	/**
	 * Override this method if you'd like to add your own object pooling.
	 *
	 * @throws Exception
	 */
	protected void setupObjectPooling() throws Exception {
		if (this.poolMap == null) {
			this.poolMap = JmxUtils.getDefaultPoolMap();

			this.poolMBeans.clear();

			for (Map.Entry<String, KeyedObjectPool> poolEntry : poolMap.entrySet()) {
				ManagedGenericKeyedObjectPool mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool) poolEntry.getValue());
				mbean.setPoolName(poolEntry.getKey());
				JmxUtils.registerJMX(mbean);
				poolMBeans.put(poolEntry.getKey(), mbean);
			}
		}
	}

	/** */
	private void validateSetup(Server server, ImmutableSet<Query> queries) throws ValidationException {
		for (Query q : queries) {
			this.validateSetup(server, q);
		}
	}

	/** */
	private void validateSetup(Server server, Query query) throws ValidationException {
		List<OutputWriter> writers = query.getOutputWriters();
		if (writers != null) {
			for (OutputWriter w : writers) {
				w.validateSetup(server, query);
			}
		}
	}

	/**
	 * Processes all the json files and manages the dedup process
	 */
	private void processFilesIntoServers(List<File> jsonFiles) throws LifecycleException {
		// Shutdown the outputwriters and clear the current server list - this gives us a clean
		// start when re-reading the json config files
		try {
			this.stopWriterAndClearMasterServerList();
		} catch (Exception e) {
			log.error("Error while clearing master server list: " + e.getMessage(), e);
			throw new LifecycleException(e);
		}

		for (File jsonFile : jsonFiles) {
			JmxProcess process;
			try {
				process = JsonUtils.getJmxProcess(jsonFile);
				if (log.isDebugEnabled()) {
					log.debug("Loaded file: " + jsonFile.getAbsolutePath());
				}
				this.masterServersList = mergeServerLists(this.masterServersList, process.getServers());
			} catch (Exception ex) {
				if (configuration.isContinueOnJsonError()) {
					throw new LifecycleException("Error parsing json: " + jsonFile, ex);
				} else {
					// error parsing one file should not prevent the startup of JMXTrans
					log.error("Error parsing json: " + jsonFile, ex);
				}
			}
		}

	}

	/**
	 * Processes all the Servers into Job's
	 * <p/>
	 * Needs to be called after processFiles()
	 */
	private void processServersIntoJobs(Scheduler scheduler) throws LifecycleException {
		for (Server server : this.masterServersList) {
			try {

				// need to inject the poolMap
				for (Query query : server.getQueries()) {
					for (OutputWriter writer : query.getOutputWriters()) {
						writer.start();
					}
				}

				// Now validate the setup of each of the OutputWriter's per
				// query.
				this.validateSetup(server, server.getQueries());

				// Now schedule the jobs for execution.
				this.scheduleJob(scheduler, server);
			} catch (ParseException ex) {
				throw new LifecycleException("Error parsing cron expression: " + server.getCronExpression(), ex);
			} catch (SchedulerException ex) {
				throw new LifecycleException("Error scheduling job for server: " + server, ex);
			} catch (ValidationException ex) {
				throw new LifecycleException("Error validating json setup for query", ex);
			}
		}
	}

	/**
	 * Schedules an individual job.
	 */
	private void scheduleJob(Scheduler scheduler, Server server) throws ParseException, SchedulerException {

		String name = server.getHost() + ":" + server.getPort() + "-" + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(10);
		JobDetail jd = new JobDetail(name, "ServerJob", ServerJob.class);

		JobDataMap map = new JobDataMap();
		map.put(Server.class.getName(), server);
		map.put(Server.JMX_CONNECTION_FACTORY_POOL, this.poolMap.get(Server.JMX_CONNECTION_FACTORY_POOL));
		jd.setJobDataMap(map);

		Trigger trigger;

		if ((server.getCronExpression() != null) && CronExpression.isValidExpression(server.getCronExpression())) {
			trigger = new CronTrigger();
			((CronTrigger) trigger).setCronExpression(server.getCronExpression());
			trigger.setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
			trigger.setStartTime(new Date());
		} else {
			Trigger minuteTrigger = TriggerUtils.makeSecondlyTrigger(configuration.getRunPeriod());
			minuteTrigger.setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
			minuteTrigger.setStartTime(new Date());

			trigger = minuteTrigger;
		}

		scheduler.scheduleJob(jd, trigger);
		if (log.isDebugEnabled()) {
			log.debug("Scheduled job: " + jd.getName() + " for server: " + server);
		}
	}

	/**
	 * Deletes all of the Jobs
	 */
	private void deleteAllJobs(Scheduler scheduler) throws Exception {
		List<JobDetail> allJobs = new ArrayList<JobDetail>();
		String[] jobGroups = scheduler.getJobGroupNames();
		for (String jobGroup : jobGroups) {
			String[] jobNames = scheduler.getJobNames(jobGroup);
			for (String jobName : jobNames) {
				allJobs.add(scheduler.getJobDetail(jobName, jobGroup));
			}
		}

		for (JobDetail jd : allJobs) {
			scheduler.deleteJob(jd.getName(), jd.getGroup());
			if (log.isDebugEnabled()) {
				log.debug("Deleted scheduled job: " + jd.getName() + " group: " + jd.getGroup());
			}
		}
	}

	/**
	 * If getJsonFile() is a file, then that is all we load. Otherwise, look in
	 * the jsonDir for files.
	 * <p/>
	 * Files must end with .json as the suffix.
	 */
	private List<File> getJsonFiles() {
		// TODO : should use a FileVisitor (Once we update to Java 7)
		File[] files;
		if ((this.configuration.getJsonDirOrFile() != null) && this.configuration.getJsonDirOrFile().isFile()) {
			files = new File[1];
			files[0] = this.configuration.getJsonDirOrFile();
		} else {
			files = this.configuration.getJsonDirOrFile().listFiles();
		}

		List<File> result = new ArrayList<File>();
		for (File file : files) {
			if (this.isJsonFile(file)) {
				result.add(file);
			}
		}
		return result;
	}

	/**
	 * Are we a file and a json file?
	 */
	private boolean isJsonFile(File file) {
		if (this.configuration.getJsonDirOrFile().isFile()) {
			return file.equals(this.configuration.getJsonDirOrFile());
		}

		return file.isFile() && file.getName().endsWith(".json");
	}

	/** */
	@Override
	public void fileModified(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			log.info("Configuration file modified: " + file);
			this.deleteAllJobs(this.serverScheduler);
			this.startupSystem();
		}
	}

	/** */
	@Override
	public void fileDeleted(File file) throws Exception {
		log.info("Configuration file deleted: " + file);
		Thread.sleep(1000);
		this.deleteAllJobs(this.serverScheduler);
		this.startupSystem();
	}

	/** */
	@Override
	public void fileAdded(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			log.info("Configuration file added: " + file);
			this.deleteAllJobs(this.serverScheduler);
			this.startupSystem();
		}
	}

	protected class ShutdownHook extends Thread {
		public void run() {
			try {
				JmxTransformer.this.stopServices();
			} catch (LifecycleException e) {
				log.error("Error shutdown hook", e);
			}
		}
	}
}
