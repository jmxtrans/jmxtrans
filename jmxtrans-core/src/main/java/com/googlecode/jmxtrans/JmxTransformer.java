/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.googlecode.jmxtrans.classloader.ClassLoaderEnricher;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.cli.JmxTransConfigurationFactory;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.monitoring.ManagedThreadPoolExecutor;
import com.googlecode.jmxtrans.scheduler.ServerScheduler;
import com.googlecode.jmxtrans.util.WatchDir;
import com.googlecode.jmxtrans.util.WatchedCallback;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Main() class that takes an argument which is the directory to look in for
 * files which contain json data that defines queries to run against JMX
 * servers.
 *
 * @author jon
 */
public class JmxTransformer implements WatchedCallback {

	private static final Logger log = LoggerFactory.getLogger(JmxTransformer.class);

	private final JmxTransConfiguration configuration;

	private final ConfigurationParser configurationParser;

	private final Injector injector;


	private WatchDir watcher;

	private ImmutableList<Server> masterServersList = ImmutableList.of();

	private Thread shutdownHook = new ShutdownHook();

	@Nonnull  private final ScheduledExecutorService reloadScheduler;

	private volatile boolean isRunning = false;
	@Nonnull private ExecutorRepository queryExecutorRepository;
	private final ServerScheduler serverScheduler;
	@Nonnull private ExecutorRepository resultExecutorRepository;
	@Nonnull private final ThreadLocalRandom random = ThreadLocalRandom.current();
	@Nonnull private final MBeanServer platformMBeanServer;
	@Nullable private ManagedJmxTransformerProcess jmxTransformerProcessMBean;
	@Nullable private ImmutableList<ManagedThreadPoolExecutor> queryExecutorMBeans;
	@Nullable private ImmutableList<ManagedThreadPoolExecutor> resultExecutorMBeans;
	private ScheduledFuture<?> reloadScheduledFuture;

	@Inject
	public JmxTransformer(
			@Nonnull ServerScheduler serverScheduler,
			JmxTransConfiguration configuration,
			ConfigurationParser configurationParser,
			Injector injector,
			@Nonnull @Named("queryExecutorRepository") ExecutorRepository queryExecutorRepository,
			@Nonnull @Named("resultExecutorRepository") ExecutorRepository resultExecutorRepository,
			@Nonnull ScheduledExecutorService scheduledExecutor
	) {
		this.serverScheduler = serverScheduler;
		this.configuration = configuration;
		this.configurationParser = configurationParser;
		this.injector = injector;
		this.queryExecutorRepository = queryExecutorRepository;
		this.resultExecutorRepository = resultExecutorRepository;
		this.reloadScheduler = scheduledExecutor;

		this.platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	public static void main(String[] args) throws Exception {
		JmxTransConfiguration configuration = JmxTransConfigurationFactory.fromArgs(args);
		if (configuration.isHelp()) {
			return;
		}

		JmxTransformer transformer = create(configuration);

		// Start the process
		transformer.doMain();
	}

	public static JmxTransformer create(JmxTransConfiguration configuration) throws MalformedURLException, FileNotFoundException {
		ClassLoaderEnricher enricher = new ClassLoaderEnricher();
		for (File jar : configuration.getAdditionalJars()) {
			enricher.add(jar);
		}

		Injector injector = JmxTransModule.createInjector(configuration);

		return injector.getInstance(JmxTransformer.class);
	}

	/**
	 * The real main method.
	 */
	private void doMain() throws Exception {
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
				log.info("shutting down", e);
				break;
			}
		}

		this.unregisterMBeans();
	}

	public synchronized void start() throws LifecycleException {
		if (isRunning) {
			throw new LifecycleException("Process already started");
		} else {
			log.info("Starting Jmxtrans on : {}", configuration.getProcessConfigDirOrFile());
			try {
				this.serverScheduler.start();

				this.startupWatchdir();
				this.initializeExecutors();
				this.registerMBeans();
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

	public synchronized void stop() throws LifecycleException {
		if (!isRunning) {
			throw new LifecycleException("Process already stopped");
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

	private synchronized void stopServices() throws LifecycleException {
		try {
			// Shutdown the scheduler
			serverScheduler.stop();

			for (ThreadPoolExecutor executor : queryExecutorRepository.getExecutors()) {
				shutdownAndAwaitTermination(executor, 10, SECONDS);
			}

			for (ThreadPoolExecutor executor : resultExecutorRepository.getExecutors()) {
				shutdownAndAwaitTermination(executor, 10, SECONDS);
			}

			// Shutdown the file watch service
			if (watcher != null) {
				watcher.stopService();
				watcher = null;
				log.debug("Shutdown watch service");
			}

			// Shutdown the outputwriters
			stopWriterAndClearMasterServerList();

		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	/**
	 * Shut down the output writers and clear the master server list
	 * Used both during shutdown and when re-reading config files
	 */
	private void stopWriterAndClearMasterServerList() {
		for (Server server : this.masterServersList) {
			for (OutputWriter writer : server.getOutputWriters()) {
				try {
					writer.close();
				} catch (LifecycleException ex) {
					log.error("Eror stopping writer: {}", writer);
				}
			}
			for (Query query : server.getQueries()) {
				for (OutputWriter writer : query.getOutputWriterInstances()) {
					try {
						writer.close();
						log.debug("Stopped writer: {} for query: {}", writer, query);
					} catch (LifecycleException ex) {
						log.error("Error stopping writer: {} for query: {}", writer, query, ex);
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
		if (this.configuration.getProcessConfigDirOrFile().isFile()) {
			dirToWatch = new File(FilenameUtils.getFullPath(this.configuration.getProcessConfigDirOrFile().getAbsolutePath()));
		} else {
			dirToWatch = this.configuration.getProcessConfigDirOrFile();
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
		Thread.sleep(MILLISECONDS.convert(10, SECONDS));
	}

	/**
	 * Processes files into Server objects and then processesServers into jobs
	 */
	private void startupSystem() throws Exception {
		this.processFilesIntoServers();
		this.processServersIntoJobs();
	}

	private void initializeExecutors() throws MalformedObjectNameException {
		this.initializeExecutors(queryExecutorRepository);
		this.initializeExecutors(resultExecutorRepository);
	}

	private void initializeExecutors(ExecutorRepository executorRepository) throws MalformedObjectNameException {
		for (Server server : this.masterServersList) {
			executorRepository.put(server);
		}
	}

	private void validateSetup(Server server, ImmutableSet<Query> queries) throws ValidationException {
		for (Query q : queries) {
			this.validateSetup(server, q);
		}
	}

	private void validateSetup(Server server, Query query) throws ValidationException {
		for (OutputWriter w : query.getOutputWriterInstances()) {
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

		this.masterServersList = configurationParser.parseServers(getProcessConfigFiles(), configuration.isContinueOnJsonError());
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
					for (OutputWriter writer : query.getOutputWriterInstances()) {
						writer.start();
					}
				}

				// Now validate the setup of each of the OutputWriter's per
				// query.
				this.validateSetup(server, server.getQueries());

				// Now schedule the jobs for execution.
				this.serverScheduler.schedule(server);
			} catch (ValidationException ex) {
				throw new LifecycleException("Error validating json setup for query", ex);
			}
		}
	}

	private void registerMBeans() throws Exception {
		jmxTransformerProcessMBean = new ManagedJmxTransformerProcess(this, configuration);
		platformMBeanServer.registerMBean(jmxTransformerProcessMBean, jmxTransformerProcessMBean.getObjectName());

		queryExecutorMBeans = registerExecutors(queryExecutorRepository);
		resultExecutorMBeans = registerExecutors(resultExecutorRepository);
	}

	private ImmutableList<ManagedThreadPoolExecutor> registerExecutors(ExecutorRepository executorRepository) throws Exception {
		ImmutableList.Builder<ManagedThreadPoolExecutor> executorMBeansBuilder = ImmutableList.builder();
		for (ManagedThreadPoolExecutor executorMBean : executorRepository.getMBeans()) {
			platformMBeanServer.registerMBean(executorMBean, executorMBean.getObjectName());
			executorMBeansBuilder.add(executorMBean);
		}

		return executorMBeansBuilder.build();
	}

	private void unregisterMBeans() throws Exception {
		if (jmxTransformerProcessMBean != null) {
			platformMBeanServer.unregisterMBean(jmxTransformerProcessMBean.getObjectName());
		}

		unregisterExecutors(queryExecutorMBeans);
		unregisterExecutors(resultExecutorMBeans);
	}

	private void unregisterExecutors(ImmutableList<ManagedThreadPoolExecutor> executorMBeans) throws Exception {
		if (executorMBeans != null) {
			for (ManagedThreadPoolExecutor executorMBean : executorMBeans) {
				platformMBeanServer.unregisterMBean(executorMBean.getObjectName());
			}
		}
	}


	@VisibleForTesting
	Date computeSpreadStartDate(int runPeriod) {
		long spread = random.nextLong(MILLISECONDS.convert(runPeriod, SECONDS));
		return new Date(new Date().getTime() + spread);
	}

	/**
	 * If getJsonFile() is a file, then that is all we load. Otherwise, look in
	 * the jsonDir for files.
	 * <p/>
	 * Files must end with .json as the suffix.
	 */
	@VisibleForTesting
	List<File> getProcessConfigFiles() {
		// TODO : should use a FileVisitor (Once we update to Java 7)
		File[] files;
		File configurationDirOrFile = configuration.getProcessConfigDirOrFile();
		if (configurationDirOrFile == null) {
			throw new IllegalStateException("Configuration should specify configuration directory or file, with -j of -f option");
		}
		if (configurationDirOrFile.isFile()) {
			files = new File[1];
			files[0] = configurationDirOrFile;
		} else {
			files = firstNonNull(configurationDirOrFile.listFiles(), new File[0]);
		}

		List<File> result = new ArrayList<>();
		for (File file : files) {
			if (this.isProcessConfigFile(file)) {
				result.add(file);
			}
		}
		return result;
	}

	/**
	 * Are we a file and a JSON or YAML file?
	 */
	private boolean isProcessConfigFile(File file) {
		if (this.configuration.getProcessConfigDirOrFile().isFile()) {
			return file.equals(this.configuration.getProcessConfigDirOrFile());
		}

		// If the file doesn't exist anymore, treat it as a regular file (to handle file deletion events)
		if(file.exists() && !file.isFile()) {
			return false;
		}

		final String fileName = file.getName();
		return !fileName.startsWith(".") && (fileName.endsWith(".json") || fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
	}

	private void scheduleReload() {
		if(this.reloadScheduledFuture != null) {
			this.reloadScheduledFuture.cancel(false);
			this.reloadScheduledFuture = null;
		}

		this.reloadScheduledFuture = reloadScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					serverScheduler.unscheduleAll();
					startupSystem();
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, 1, TimeUnit.SECONDS);
	}

	private void handleFileEvent(File file, String event) throws Exception {
		if (this.isProcessConfigFile(file)) {
			log.info("Configuration file {}: {}", event, file);
			scheduleReload();
		}
	}

	@Override
	public void fileModified(File file) throws Exception {
		handleFileEvent(file, "modified");
	}

	@Override
	public void fileDeleted(File file) throws Exception {
		handleFileEvent(file, "deleted");
	}

	@Override
	public void fileAdded(File file) throws Exception {
		handleFileEvent(file, "added");
	}

	protected class ShutdownHook extends Thread {
		@Override
		public void run() {
			try {
				JmxTransformer.this.stopServices();
			} catch (LifecycleException e) {
				log.error("Error shutdown hook", e);
			}
		}
	}
}
