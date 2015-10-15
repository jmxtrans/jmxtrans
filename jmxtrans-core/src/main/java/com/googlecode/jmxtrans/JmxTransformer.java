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
package com.googlecode.jmxtrans;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.classloader.ClassLoaderEnricher;
import com.googlecode.jmxtrans.cli.CliArgumentParser;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.jobs.ServerJob;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.util.WatchDir;
import com.googlecode.jmxtrans.util.WatchedCallback;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Main() class that takes an argument which is the directory to look in for
 * files which contain json data that defines queries to run against JMX
 * servers.
 *
 * @author jon
 */
public class JmxTransformer implements WatchedCallback {

	private static final Logger log = LoggerFactory.getLogger(JmxTransformer.class);

	private final Scheduler serverScheduler;

	private final JmxTransConfiguration configuration;

	private final ConfigurationParser configurationParser;

	private final Injector injector;

	private WatchDir watcher;

	private ImmutableList<Server> masterServersList = ImmutableList.of();

	/**
	 * The shutdown hook.
	 */
	private Thread shutdownHook = new ShutdownHook();

	private volatile boolean isRunning = false;

	@Inject
	public JmxTransformer(Scheduler serverScheduler, JmxTransConfiguration configuration, ConfigurationParser configurationParser, Injector injector) {
		this.serverScheduler = serverScheduler;
		this.configuration = configuration;
		this.configurationParser = configurationParser;
		this.injector = injector;
	}

	public static void main(String[] args) throws Exception {
		JmxTransConfiguration configuration = new CliArgumentParser().parseOptions(args);
		if (configuration.isHelp()) {
			return;
		}

		ClassLoaderEnricher enricher = new ClassLoaderEnricher();
		for (File jar : configuration.getAdditionalJars()) {
			enricher.add(jar);
		}

		Injector injector = Guice.createInjector(new JmxTransModule(configuration));

		JmxTransformer transformer = injector.getInstance(JmxTransformer.class);

		// Start the process
		transformer.doMain();
	}

	/**
	 * The real main method.
	 */
	private void doMain() throws Exception {
		ManagedJmxTransformerProcess mbean = new ManagedJmxTransformerProcess(this, configuration);
		ManagementFactory.getPlatformMBeanServer()
				.registerMBean(mbean, mbean.getObjectName());

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

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.unregisterMBean(mbean.getObjectName());
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
				this.serverScheduler.start();

				this.startupWatchdir();

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
			if (this.serverScheduler.isStarted()) {
				this.serverScheduler.shutdown(true);
				log.debug("Shutdown server scheduler");
				try {
					// FIXME: Quartz issue, need to sleep
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
				}
			}

			// Shutdown the file watch service
			if (this.watcher != null) {
				this.watcher.stopService();
				this.watcher = null;
				log.debug("Shutdown watch service");
			}

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
		this.masterServersList = ImmutableList.of();
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
	 * Handy method which runs the JmxProcess
	 */
	public void executeStandalone(JmxProcess process) throws Exception {
		this.masterServersList = process.getServers();

		this.serverScheduler.start();

		this.processServersIntoJobs();

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
		this.processFilesIntoServers();

		// process the servers into jobs
		this.processServersIntoJobs();
	}

	private void validateSetup(Server server, ImmutableSet<Query> queries) throws ValidationException {
		for (Query q : queries) {
			this.validateSetup(server, q);
		}
	}

	private void validateSetup(Server server, Query query) throws ValidationException {
		List<OutputWriter> writers = query.getOutputWriters();
		for (OutputWriter w : writers) {
			injector.injectMembers(w);
			w.validateSetup(server, query);
		}
	}

	/**
	 * Processes all the json files and manages the dedup process
	 */
	private void processFilesIntoServers() throws LifecycleException {
		// Shutdown the outputwriters and clear the current server list - this gives us a clean
		// start when re-reading the json config files
		try {
			this.stopWriterAndClearMasterServerList();
		} catch (Exception e) {
			log.error("Error while clearing master server list: " + e.getMessage(), e);
			throw new LifecycleException(e);
		}

		this.masterServersList = configurationParser.parseServers(getJsonFiles(), configuration.isContinueOnJsonError());
	}

	/**
	 * Processes all the Servers into Job's
	 * <p/>
	 * Needs to be called after processFiles()
	 */
	private void processServersIntoJobs() throws LifecycleException {
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
				this.scheduleJob(server);
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
	private void scheduleJob(Server server) throws ParseException, SchedulerException {

		String name = server.getHost() + ":" + server.getPort() + "-" + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(10);
		JobDetail jd = new JobDetail(name, "ServerJob", ServerJob.class);

		JobDataMap map = new JobDataMap();
		map.put(Server.class.getName(), server);
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

		serverScheduler.scheduleJob(jd, trigger);
		if (log.isDebugEnabled()) {
			log.debug("Scheduled job: " + jd.getName() + " for server: " + server);
		}
	}

	/**
	 * Deletes all of the Jobs
	 */
	private void deleteAllJobs() throws Exception {
		List<JobDetail> allJobs = new ArrayList<JobDetail>();
		String[] jobGroups = serverScheduler.getJobGroupNames();
		for (String jobGroup : jobGroups) {
			String[] jobNames = serverScheduler.getJobNames(jobGroup);
			for (String jobName : jobNames) {
				allJobs.add(serverScheduler.getJobDetail(jobName, jobGroup));
			}
		}

		for (JobDetail jd : allJobs) {
			serverScheduler.deleteJob(jd.getName(), jd.getGroup());
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
		File jsonDirOrFile = configuration.getJsonDirOrFile();
		if (jsonDirOrFile == null) {
			throw new IllegalStateException("Configuration should specify configuration directory or file, with -j of -f option");
		}
		if (jsonDirOrFile.isFile()) {
			files = new File[1];
			files[0] = jsonDirOrFile;
		} else {
			files = firstNonNull(jsonDirOrFile.listFiles(), new File[0]);
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

	@Override
	public void fileModified(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			log.info("Configuration file modified: " + file);
			this.deleteAllJobs();
			this.startupSystem();
		}
	}

	@Override
	public void fileDeleted(File file) throws Exception {
		log.info("Configuration file deleted: " + file);
		Thread.sleep(1000);
		this.deleteAllJobs();
		this.startupSystem();
	}

	@Override
	public void fileAdded(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			log.info("Configuration file added: " + file);
			this.deleteAllJobs();
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
