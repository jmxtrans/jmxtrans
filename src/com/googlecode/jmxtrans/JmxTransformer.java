package com.googlecode.jmxtrans;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.SignalInterceptor;

/**
 * Main() class that takes an argument which is the directory 
 * to look in for files which contain json data that defines 
 * queries to run against JMX servers.
 * 
 * @author jon
 */
public class JmxTransformer extends SignalInterceptor {
    private File jsonDir = new File(".");
    private int numFileThreads = 0;
    private boolean runEndlessly = false;
    private boolean isRunning = false;
    
    /** */
    public static void main(String[] args) throws Exception {
        JmxTransformer transformer = new JmxTransformer();
        transformer.register("TERM");
        transformer.register("INT");
        transformer.register("KILL");
        transformer.register("ABRT");
        transformer.doMain(args);
        return;
    }

    /**
     * The real main method.
     */
    private void doMain(String[] args) throws Exception {
        if (!parseOptions(args)) {
            return;
        }

        while (runEndlessly) {
            isRunning = true;
            execute();
            isRunning = false;
            Thread.sleep(1000 * 60);
        }

        if (!runEndlessly) {
            isRunning = true;
            execute();
            isRunning = false;
        }
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
            } else if (option.getOpt().equals("t")) {
                setNumFileThreads(Integer.valueOf(option.getValue()));
            } else if (option.getOpt().equals("e")) {
                setRunEndlessly(true);
            } else if (option.getOpt().equals("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar jmxtrans-all.jar", getOptions());
                result = false;
            }
        }
        return result;
    }

    /**
     * Deals with either starting threads to process files or process
     * them sequentially.
     */
    private void execute() throws Exception {
        if (numFileThreads > 0) {
            ExecutorService service = null;
            try {
                service = Executors.newFixedThreadPool(numFileThreads);
                List<Callable<Object>> threads = new ArrayList<Callable<Object>>();
                for (File file : getJsonFiles()) {
                    ProcessFileThread pqt = new ProcessFileThread(this, file);
                    threads.add(Executors.callable(pqt));
                }
                service.invokeAll(threads);
            } finally {
                service.shutdown();
            }
        } else {
            for (File file : getJsonFiles()) {
                processJsonFile(file);
            }
        }
    }
    
    /** */
    public Options getOptions() {
        Options options = new Options();
        options.addOption("j", true, "Directory where json configuration is stored. Default is .");
        options.addOption("t", true, "Maximum number of threads to start for processing each json file. Default is 0.");
        options.addOption("e", false, "Run endlessly. Default false.");
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
    private List<File> getJsonFiles() {
        File[] files = jsonDir.listFiles();
        List<File> result = new ArrayList<File>();
        for (File file : files) {
            if (file.getName().endsWith(".json")) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * We read a directory full of .json files. If this value is greater than
     * zero, start up a maximum number of threads to process each file.
     */
    public void setNumFileThreads(int numFileThreads) {
        this.numFileThreads = numFileThreads;
    }

    /**
     * We read a directory full of .json files. If this value is greater than
     * zero, start up a maximum number of threads to process each file.
     */
    public int getNumFileThreads() {
        return numFileThreads;
    }

    /**
     * A thread for processing a file.
     */
    private static class ProcessFileThread implements Runnable {
        private File jsonFile;
        private JmxTransformer transformer;

        public ProcessFileThread(JmxTransformer transformer, File jsonFile) {
            this.transformer = transformer;
            this.jsonFile = jsonFile;
        }
        
        public void run() {
            try {
                transformer.processJsonFile(jsonFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This is what does the work.
     */
    public void processJsonFile(File jsonFile) throws Exception{
        JmxProcess process = JmxUtils.getJmxProcess(jsonFile);
        JmxUtils.execute(process);
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
     * Waits until the execution finishes before stopping.
     */
    @Override
    protected boolean handle(String signame) {
        while (this.isRunning) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignored
            }
        }
        return true;
    }
}
