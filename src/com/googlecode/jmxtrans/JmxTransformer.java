package com.googlecode.jmxtrans;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.pool.KeyedObjectPool;
import org.codehaus.jackson.annotate.JsonIgnore;
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

import com.googlecode.jmxtrans.jobs.ServerJob;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.OptionsException;
import com.googlecode.jmxtrans.util.SignalInterceptor;
import com.googlecode.jmxtrans.util.ValidationException;
import com.googlecode.jmxtrans.util.WatchDir;
import com.googlecode.jmxtrans.util.WatchedCallback;

/**
 * Main() class that takes an argument which is the directory
 * to look in for files which contain json data that defines
 * queries to run against JMX servers.
 *
 * @author jon
 */
public class JmxTransformer extends SignalInterceptor implements WatchedCallback {

	private static final Logger log = LoggerFactory.getLogger(JmxTransformer.class);

	private static String QUARTZ_SERVER_PROPERTIES = null;
	private static int SECONDS_BETWEEN_SERVER_JOB_RUNS = 60;

	private File jsonDirOrFile;

	private boolean runEndlessly;
	private Scheduler serverScheduler;

	private WatchDir watcher;

	private Map<String, KeyedObjectPool> poolMap;

	private List<Server> masterServersList = new ArrayList<Server>();


	/** */
	public static void main(String[] args) throws Exception {
		JmxTransformer transformer = new JmxTransformer();

		// Register signal handlers
        transformer.registerQuietly("HUP");
        transformer.registerQuietly("INT");
        transformer.registerQuietly("ABRT");
        transformer.registerQuietly("KILL");
		transformer.registerQuietly("TERM");

		// Start the process
		transformer.doMain(args);
	}

	/**
	 * The real main method.
	 */
	private void doMain(String[] args) throws Exception {
		if (!this.parseOptions(args)) {
			return;
		}

		try {
		    this.startupScheduler();

		    this.startupWatchdir();

			this.setupObjectPooling();

			this.startupSystem();

		} catch (Exception e) {
			this.runEndlessly = false;
			log.error(e.getMessage(), e.getCause());
		}

		if (!this.runEndlessly) {
			this.handle("TERM");
		}
	}

	/**
	 * Startup the watchdir service.
	 */
	private void startupWatchdir() throws Exception {
        File dirToWatch = null;
        if (this.getJsonDirOrFile().isFile()) {
            dirToWatch = new File(FilenameUtils.getFullPath(this.getJsonDirOrFile().getAbsolutePath()));
        } else {
            dirToWatch = this.getJsonDirOrFile();
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
        InputStream stream = null;
        if (QUARTZ_SERVER_PROPERTIES == null) {
            QUARTZ_SERVER_PROPERTIES = "/quartz.server.properties";
            stream = JmxTransformer.class.getResourceAsStream(QUARTZ_SERVER_PROPERTIES);
        } else {
            stream = new FileInputStream(QUARTZ_SERVER_PROPERTIES);
        }
        serverSchedFact.initialize(stream);
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

	    Thread.sleep(10 * 1000);
	    this.handle("TERM");
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
	 */
	protected void setupObjectPooling() {
		if (this.poolMap == null) {
			this.poolMap = JmxUtils.getDefaultPoolMap();
		}
	}

	/**
	 * Allows you to modify the object pool map.
	 */
	@JsonIgnore
	public Map<String, KeyedObjectPool> getObjectPoolMap() {
		if (this.poolMap == null) {
			this.setupObjectPooling();
		}
		return this.poolMap;
	}

	/** */
	private void validateSetup(List<Query> queries) throws ValidationException {
		for (Query q : queries) {
			this.validateSetup(q);
		}
	}

	/** */
	private void validateSetup(Query query) throws ValidationException {
		List<OutputWriter> writers = query.getOutputWriters();
		if (writers != null) {
			for (OutputWriter w : writers) {
				w.validateSetup(query);
			}
		}
	}

	/**
	 * Processes all the json files and manages the dedup process
	 */
	private void processFilesIntoServers(List<File> jsonFiles) throws LifecycleException {

		for (File jsonFile : jsonFiles) {
			JmxProcess process;
			try {
				process = JmxUtils.getJmxProcess(jsonFile);
				if (log.isDebugEnabled()) {
					log.debug("Loaded file: " + jsonFile.getAbsolutePath());
				}
				JmxUtils.mergeServerLists(this.masterServersList, process.getServers());
			} catch (Exception ex) {
				throw new LifecycleException("Error parsing json: " + jsonFile, ex);
			}
		}

	}

	/**
	 * Processes all the Servers into Job's
	 *
	 * Needs to be called after processFiles()
	 */
	private void processServersIntoJobs(Scheduler scheduler) throws LifecycleException {
		for (Server server : this.masterServersList) {
			try {

				// need to inject the poolMap
				for (Query query : server.getQueries()) {
					query.setServer(server);

					for (OutputWriter writer : query.getOutputWriters()) {
						writer.setObjectPoolMap(this.poolMap);
						writer.start();
					}
				}

				// Now validate the setup of each of the OutputWriter's per query.
				this.validateSetup(server.getQueries());

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

		String name = server.getHost() + ":" + server.getPort() + "-" + System.currentTimeMillis();
		JobDetail jd = new JobDetail(name, "ServerJob", ServerJob.class);

		JobDataMap map = new JobDataMap();
		map.put(Server.class.getName(), server);
		map.put(Server.JMX_CONNECTION_FACTORY_POOL, this.getObjectPoolMap().get(Server.JMX_CONNECTION_FACTORY_POOL));
		jd.setJobDataMap(map);

		Trigger trigger = null;

		if ((server.getCronExpression() != null) && CronExpression.isValidExpression(server.getCronExpression())) {
			trigger = new CronTrigger();
			((CronTrigger)trigger).setCronExpression(server.getCronExpression());
			((CronTrigger)trigger).setName(server.getHost() + ":" + server.getPort() + "-" + Long.valueOf(System.currentTimeMillis()).toString());
			((CronTrigger)trigger).setStartTime(new Date());
		} else {
			Trigger minuteTrigger = TriggerUtils.makeSecondlyTrigger(SECONDS_BETWEEN_SERVER_JOB_RUNS);
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
	 * If this is true, then this class will execute the main()
	 * loop and then wait 60 seconds until running again.
	 */
	public void setRunEndlessly(boolean runEndlessly) {
		this.runEndlessly = runEndlessly;
	}

	public boolean isRunEndlessly() {
		return this.runEndlessly;
	}

	/**
	 * Parse the options given on the command line.
	 */
	private boolean parseOptions(String[] args) throws OptionsException, org.apache.commons.cli.ParseException {
		CommandLineParser parser = new GnuParser();
		CommandLine cl = parser.parse(this.getOptions(), args);
		Option[] options = cl.getOptions();

		boolean result = true;

		for (Option option : options) {
			if (option.getOpt().equals("j")) {
				File tmp = new File(option.getValue());
				if (!tmp.exists() && !tmp.isDirectory()) {
					throw new OptionsException("Path to json directory is invalid: " + tmp);
				}
				this.setJsonDirOrFile(tmp);
			} else if (option.getOpt().equals("f")) {
				File tmp = new File(option.getValue());
				if (!tmp.exists() && !tmp.isFile()) {
					throw new OptionsException("Path to json file is invalid: " + tmp);
				}
				this.setJsonDirOrFile(tmp);
			} else if (option.getOpt().equals("e")) {
				this.setRunEndlessly(true);
			} else if (option.getOpt().equals("q")) {
				QUARTZ_SERVER_PROPERTIES = option.getValue();
				File file = new File(QUARTZ_SERVER_PROPERTIES);
				if (!file.exists()) {
					throw new OptionsException("Could not find path to the quartz properties file: " + file.getAbsolutePath());
				}
			} else if (option.getOpt().equals("s")) {
				SECONDS_BETWEEN_SERVER_JOB_RUNS = Integer.valueOf(option.getValue());
			} else if (option.getOpt().equals("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar jmxtrans-all.jar", this.getOptions());
				result = false;
			}
		}
		if ((result == true) && (this.getJsonDirOrFile() == null)) {
			throw new OptionsException("Please specify either the -f or -j option.");
		}
		return result;
	}

	/** */
	public Options getOptions() {
		Options options = new Options();
		options.addOption("j", true, "Directory where json configuration is stored. Default is .");
		options.addOption("f", true, "A single json file to execute.");
		options.addOption("e", false, "Run endlessly. Default false.");
		options.addOption("q", true, "Path to quartz configuration file.");
		options.addOption("s", true, "Seconds between server job runs (not defined with cron). Default: 60");
		options.addOption("h", false, "Help");
		return options;
	}

	/** */
	public void setJsonDirOrFile(File jsonDirOrFile) {
		this.jsonDirOrFile = jsonDirOrFile;
	}

	/** */
	public File getJsonDirOrFile() {
		return this.jsonDirOrFile;
	}

	/**
	 * If getJsonFile() is a file, then that is all we load. Otherwise,
	 * look in the jsonDir for files.
	 *
	 * Files must end with .json as the suffix.
	 */
	private List<File> getJsonFiles() {
		File[] files = null;
		if ((this.getJsonDirOrFile() != null) && this.getJsonDirOrFile().isFile()) {
			files = new File[1];
			files[0] = this.getJsonDirOrFile();
		} else {
			files = this.getJsonDirOrFile().listFiles();
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
	 * Waits until the execution finishes before stopping.
	 */
	@Override
	protected boolean handle(String signame) {
		if (log.isDebugEnabled()) {
			log.debug("Got signame: " + signame);
		}
		try {

			// Shutdown the scheduler
			if (this.serverScheduler != null) {
				this.serverScheduler.shutdown(true);
				log.debug("Shutdown server scheduler");
			}

			// Shutdown the file watch service
			if (this.watcher != null) {
				this.watcher.stopService();
				log.debug("Shutdown watch service");
			}

			// Shutdown the pools
			for (Entry<String, KeyedObjectPool> entry : this.getObjectPoolMap().entrySet()) {
				try {
					entry.getValue().close();
                    log.debug("Closed object pool factory: " + entry.getKey());
				} catch (Exception ex) {
                    log.error("Error closing object pool factory: " + entry.getKey());
				}
			}

			// Shutdown the outputwriters
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

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	/**
	 * Are we a file and a json file?
	 */
	private boolean isJsonFile(File file) {
		if (this.getJsonDirOrFile().isFile()) {
			return file.equals(this.getJsonDirOrFile());
		}
		return file.isFile() && file.getName().endsWith(".json");
	}

	/** */
	public void fileModified(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			if (log.isDebugEnabled()) {
				log.debug("File modified: " + file);
			}
			this.deleteAllJobs(this.serverScheduler);
			this.startupSystem();
		}
	}

	/** */
	public void fileDeleted(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			if (log.isDebugEnabled()) {
				log.debug("File deleted: " + file);
			}
			this.deleteAllJobs(this.serverScheduler);
			this.startupSystem();
		}
	}

	/** */
	public void fileAdded(File file) throws Exception {
		if (this.isJsonFile(file)) {
			Thread.sleep(1000);
			if (log.isDebugEnabled()) {
				log.debug("File added: " + file);
			}
			this.startupSystem();
		}
	}
}
