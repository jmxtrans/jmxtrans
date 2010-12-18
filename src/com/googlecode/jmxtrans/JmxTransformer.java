package com.googlecode.jmxtrans;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
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
    
    private File jsonDir = new File(".");
    private boolean runEndlessly = false;
    private Scheduler serverScheduler = null;
    
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

        watcher = new WatchDir(jsonDir, this);
        watcher.start();

        for (File file : getJsonFiles(getJsonDir())) {
            scheduleJobsInFile(serverScheduler, file);
        }

        if (!runEndlessly) {
            this.handle("TERM");
        }
    }

    /**
     * Sucks in a File and creates jobs for each Server in the file.
     */
    private void scheduleJobsInFile(Scheduler scheduler, File jsonFile) throws Exception {
        JmxProcess process = JmxUtils.getJmxProcess(jsonFile);

        for (Server server : process.getServers()) {
            scheduleJob(scheduler, jsonFile, server);
        }
    }
    
    /**
     * Schedules an individual job.
     */
    private void scheduleJob(Scheduler scheduler, File jsonFile, Server server) throws Exception {
        
        // The Jobs name is related to the file it was contained in.
        String name = jsonFile.getCanonicalPath() + "-" + server.getHost() + ":" + server.getPort() + "-" + System.currentTimeMillis();
        JobDetail jd = new JobDetail(name, "ServerJob", ServerJob.class);

        JobDataMap map = new JobDataMap();
        map.put("server", server);
        jd.setJobDataMap(map);

        Trigger trigger = null;
        
        if (server.getCronExpression() != null && CronExpression.isValidExpression(server.getCronExpression())) {
            trigger = new CronTrigger();
            ((CronTrigger)trigger).setCronExpression(server.getCronExpression());
            ((CronTrigger)trigger).setName(Long.valueOf(System.currentTimeMillis()).toString());
            ((CronTrigger)trigger).setStartTime(new Date());
        } else {
            Trigger minuteTrigger = TriggerUtils.makeSecondlyTrigger(SECONDS_BETWEEN_SERVER_JOB_RUNS);
            minuteTrigger.setName(Long.valueOf(System.currentTimeMillis()).toString());
            minuteTrigger.setStartTime(new Date());

            trigger = minuteTrigger;
        }

        scheduler.scheduleJob(jd, trigger);
        if (log.isDebugEnabled()) {
            log.debug("Scheduled job: " + jd.getName() + " for server: " + server);
        }
    }

    /** 
     * Deletes all of the Jobs related to a file.
     */
    private void deleteJobsInFile(Scheduler scheduler, File jsonFile) throws Exception {
        List<JobDetail> allJobs = new ArrayList<JobDetail>();
        String[] jobGroups = scheduler.getJobGroupNames();
        for (String jobGroup : jobGroups) {
            String[] jobNames = scheduler.getJobNames(jobGroup);
            for (String jobName : jobNames) {
                allJobs.add(scheduler.getJobDetail(jobName, jobGroup));
            }
        }

        for (JobDetail jd : allJobs) {
            if (jd.getName().startsWith(jsonFile.getCanonicalPath() + "-")) {
                scheduler.deleteJob(jd.getName(), jd.getGroup());
                if (log.isDebugEnabled()) {
                    log.debug("Deleted scheduled job: " + jd.getName() + " group: " + jd.getGroup());
                }
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

    /** */
    private boolean parseOptions(String[] args) throws Exception {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(getOptions(), args);
        Option[] options = cl.getOptions();

        boolean result = true;
        
        for (Option option : options) {
            if (option.getOpt().equals("j")) {
                File tmp = new File(option.getValue());
                if (!tmp.exists() && !tmp.isDirectory()) {
                    throw new RuntimeException("Path to json directory is invalid: " + tmp);
                }
                setJsonDir(tmp);
            } else if (option.getOpt().equals("e")) {
                setRunEndlessly(true);
            } else if (option.getOpt().equals("q")) {
                QUARTZ_SERVER_PROPERTIES = option.getValue();
                File file = new File(QUARTZ_SERVER_PROPERTIES);
                if (!file.exists()) {
                    throw new RuntimeException("Could not find path to the quartz properties file: " + file.getCanonicalPath());
                }
            } else if (option.getOpt().equals("s")) {
                SECONDS_BETWEEN_SERVER_JOB_RUNS = Integer.valueOf(option.getValue());
            } else if (option.getOpt().equals("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar jmxtrans-all.jar", getOptions());
                result = false;
            }
        }
        return result;
    }

    /** */
    public Options getOptions() {
        Options options = new Options();
        options.addOption("j", true, "Directory where json configuration is stored. Default is .");
        options.addOption("e", false, "Run endlessly. Default false.");
        options.addOption("q", true, "Path to quartz configuration file.");
        options.addOption("s", true, "Seconds between server job runs (not defined with cron). Default: 60");
        options.addOption("h", false, "Help");
        return options;
    }

    /** */
    public void setJsonDir(File jsonDir) {
        this.jsonDir = jsonDir;
    }

    /** */
    public File getJsonDir() {
        return jsonDir;
    }
    
    /**
     * Looks in the jsonDir for files that end with .json as the suffix.
     */
    private List<File> getJsonFiles(File jsonDir) {
        File[] files = jsonDir.listFiles();
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
        return file.isFile() && file.getName().endsWith(".json");
    }

    /** */
    public void fileModified(File file) throws Exception {
        if (isJsonFile(file)) {
            deleteJobsInFile(serverScheduler, file);
            scheduleJobsInFile(serverScheduler, file);
            if (log.isDebugEnabled()) {
                log.debug("File modified: " + file);
            }
        }
    }

    /** */
    public void fileDeleted(File file) throws Exception {
        if (isJsonFile(file)) {
            deleteJobsInFile(serverScheduler, file);
            if (log.isDebugEnabled()) {
                log.debug("File deleted: " + file);
            }
        }
    }

    /** */
    public void fileAdded(File file) throws Exception {
        if (isJsonFile(file)) {
            scheduleJobsInFile(serverScheduler, file);
            if (log.isDebugEnabled()) {
                log.debug("File added: " + file);
            }
        }
    }
}
