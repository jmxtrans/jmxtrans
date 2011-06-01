package com.googlecode.jmxtrans;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
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
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;
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

    /** */
    public static void main(String[] args) throws Exception {
        JmxTransformer transformer = new JmxTransformer();
        transformer.registerQuietly("TERM");
        transformer.registerQuietly("INT");
        transformer.registerQuietly("ABRT");
        transformer.registerQuietly("KILL");
        try {
            transformer.doMain(args);
        } catch (Exception e) {
            log.error("Error", e);
            throw new Exception(e);
        }
        return;
    }

    /**
     * The real main method.
     */
    private void doMain(String[] args) throws Exception {
        if (!parseOptions(args)) {
            return;
        }
        // start the server scheduler which loops over all the Server jobs
        StdSchedulerFactory serverSchedFact = new StdSchedulerFactory();
        InputStream stream = null;
        if (QUARTZ_SERVER_PROPERTIES == null) {
            QUARTZ_SERVER_PROPERTIES = "/quartz.server.properties";
            stream = JmxTransformer.class.getResourceAsStream(QUARTZ_SERVER_PROPERTIES);
        } else {
            stream = new FileInputStream(QUARTZ_SERVER_PROPERTIES);
        }
        serverSchedFact.initialize(stream);
        serverScheduler = serverSchedFact.getScheduler();
        serverScheduler.start();

        File dirToWatch = null;
        if (getJsonDirOrFile().isFile()) {
        	dirToWatch = new File(FilenameUtils.getFullPath(getJsonDirOrFile().getAbsolutePath()));
        } else {
        	dirToWatch = getJsonDirOrFile();
        }

        watcher = new WatchDir(dirToWatch, this);
        watcher.start();

        processFiles(serverScheduler, getJsonFiles());
        
        if (!runEndlessly) {
            this.handle("TERM");
        }
    }

    /**
     * Adds files, which turn into jobs into the scheduler.
     */
    private void processFiles(Scheduler scheduler, List<File> jsonFiles) {

    	List<Server> mergedServers = new ArrayList<Server>();

    	for (File jsonFile : jsonFiles) {
            JmxProcess process;
			try {
				process = JmxUtils.getJmxProcess(jsonFile);
	            JmxUtils.mergeServerLists(mergedServers, process.getServers());
			} catch (Exception ex) {
				log.error("Error parsing json: " + jsonFile, ex);
			}
        }

        for (Server server : mergedServers) {
            try {
				scheduleJob(scheduler, server);
			} catch (ParseException ex) {
				log.error("Error parsing cron expression: " + server.getCronExpression(), ex);
			} catch (SchedulerException ex) {
				log.error("Error scheduling job for server: " + server, ex);
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
        map.put("server", server);
        jd.setJobDataMap(map);

        Trigger trigger = null;

        if (server.getCronExpression() != null && CronExpression.isValidExpression(server.getCronExpression())) {
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
        return runEndlessly;
    }

    /**
     * Parse the options given on the command line.
     */
    private boolean parseOptions(String[] args) throws ValidationException, org.apache.commons.cli.ParseException {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(getOptions(), args);
        Option[] options = cl.getOptions();

        boolean result = true;

        for (Option option : options) {
            if (option.getOpt().equals("j")) {
                File tmp = new File(option.getValue());
                if (!tmp.exists() && !tmp.isDirectory()) {
                    throw new ValidationException("Path to json directory is invalid: " + tmp);
                }
                setJsonDirOrFile(tmp);
            } else if (option.getOpt().equals("f")) {
                File tmp = new File(option.getValue());
                if (!tmp.exists() && !tmp.isFile()) {
                    throw new ValidationException("Path to json file is invalid: " + tmp);
                }
                setJsonDirOrFile(tmp);
            } else if (option.getOpt().equals("e")) {
                setRunEndlessly(true);
            } else if (option.getOpt().equals("q")) {
                QUARTZ_SERVER_PROPERTIES = option.getValue();
                File file = new File(QUARTZ_SERVER_PROPERTIES);
                if (!file.exists()) {
                    throw new ValidationException("Could not find path to the quartz properties file: " + file.getAbsolutePath());
                }
            } else if (option.getOpt().equals("s")) {
                SECONDS_BETWEEN_SERVER_JOB_RUNS = Integer.valueOf(option.getValue());
            } else if (option.getOpt().equals("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar jmxtrans-all.jar", getOptions());
                result = false;
            }
        }
        if (result == true && this.getJsonDirOrFile() == null) {
        	throw new ValidationException("Please specify either the -f or -j option.");
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
		return jsonDirOrFile;
	}

	/**
     * If getJsonFile() is a file, then that is all we load. Otherwise,
     * look in the jsonDir for files. 
     * 
     * Files must end with .json as the suffix.
     */
    private List<File> getJsonFiles() {
    	File[] files = null;
    	if (getJsonDirOrFile() != null && getJsonDirOrFile().isFile()) {
    		files = new File[1];
    		files[0] = getJsonDirOrFile();
    	} else {
            files = getJsonDirOrFile().listFiles();
    	}

    	List<File> result = new ArrayList<File>();
        for (File file : files) {
            if (isJsonFile(file)) {
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
            if (serverScheduler != null) {
                serverScheduler.shutdown(true);
                log.debug("Shutdown server scheduler!");
            }
            if (watcher != null) {
                watcher.stopService();
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
    	if (getJsonDirOrFile().isFile()) {
    		return file.equals(getJsonDirOrFile());
    	}
        return file.isFile() && file.getName().endsWith(".json");
    }

    /** */
    public void fileModified(File file) throws Exception {
        if (isJsonFile(file)) {
            Thread.sleep(1000);
            deleteAllJobs(serverScheduler);
            processFiles(serverScheduler, getJsonFiles());
            if (log.isDebugEnabled()) {
                log.debug("File modified: " + file);
            }
        }
    }

    /** */
    public void fileDeleted(File file) throws Exception {
        if (isJsonFile(file)) {
            Thread.sleep(1000);
            deleteAllJobs(serverScheduler);
            processFiles(serverScheduler, getJsonFiles());
            if (log.isDebugEnabled()) {
                log.debug("File deleted: " + file);
            }
        }
    }

    /** */
    public void fileAdded(File file) throws Exception {
        if (isJsonFile(file)) {
            Thread.sleep(1000);
            processFiles(serverScheduler, getJsonFiles());
            if (log.isDebugEnabled()) {
                log.debug("File added: " + file);
            }
        }
    }
}
