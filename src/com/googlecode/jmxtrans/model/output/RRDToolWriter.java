package com.googlecode.jmxtrans.model.output;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdDefTemplate;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;

/**
 * This takes a JRobin template.xml file and then creates the database if it doesn't already exist.
 * 
 * It will then write the contents of the Query (the Results) to the database.
 * 
 * This method exec's out to use the command line version of rrdtool. You need to specify the
 * path to the directory where the binary rrdtool lives.
 * 
 * @author jon
 */
public class RRDToolWriter extends BaseOutputWriter {

    /** */
    public RRDToolWriter() {
    }

    public void validateSetup() throws Exception {
        if (this.getOutputFile() == null || this.getTemplateFile() == null || this.getBinaryPath() == null) {
            throw new RuntimeException("output, template and binary path file can't be null");
        }
    }

    /** */
    public void doWrite(Query query) throws Exception {
        RrdDef def = getDatabaseTemplateSpec();

        List<String> dsNames = getDsNames(def.getDsDefs());
        List<Result> results = query.getResults();

        Map<String, String> dataMap = new TreeMap<String, String>();

        // go over all the results and look for datasource names that map to keys from the result values
        for (Result res : results) {
            Map<String, Object> values = res.getValues();
            for (Entry<String, Object> entry : values.entrySet()) {
                if (dsNames.contains(entry.getKey())) {
                    dataMap.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        
        rrdToolUpdate(StringUtils.join(dataMap.keySet(), ':'), StringUtils.join(dataMap.values(), ':'));
    }

    /**
     * Executes the rrdtool update command.
     */
    protected void rrdToolUpdate(String template, String data) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(this.getBinaryPath() + "/rrdtool");
        commands.add("update");
        commands.add(this.getOutputFile().getCanonicalPath());
        commands.add("-t");
        commands.add(template);
        commands.add("N:" + data);

        ProcessBuilder pb = new ProcessBuilder(commands);
        Process process = pb.start();
        checkErrorStream(process);
    }
    
    /**
     * If the database file doesn't exist, it'll get created, otherwise, it'll be returned in r/w mode.
     */
    protected RrdDef getDatabaseTemplateSpec() throws Exception {
        RrdDefTemplate t = new RrdDefTemplate(this.getTemplateFile());
        t.setVariable("database", this.getOutputFile().getCanonicalPath());
        RrdDef def = t.getRrdDef();
        if (!this.getOutputFile().exists()) {
            FileUtils.forceMkdir(this.getOutputFile().getParentFile());
            rrdToolCreateDatabase(def);
        }
        return def;
    }
    
    /**
     * Calls out to the rrdtool binary with the 'create' command.
     */
    protected void rrdToolCreateDatabase(RrdDef def) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(this.getBinaryPath() + "/rrdtool");
        commands.add("create");
        commands.add(this.getOutputFile().getCanonicalPath());
        commands.add("-s");
        commands.add(String.valueOf(def.getStep()));
        
        for (DsDef dsdef : def.getDsDefs()) {
            commands.add(getDsDefStr(dsdef));
        }
        
        for (ArcDef adef : def.getArcDefs()) {
            commands.add(getRraStr(adef));
        }
        
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process process = pb.start();
        checkErrorStream(process);
    }
    
    /**
     * Check to see if there was an error processing an rrdtool command
     */
    private void checkErrorStream(Process process) throws Exception {
        InputStream is = process.getErrorStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        if (sb.length() > 0) {
            throw new RuntimeException(sb.toString());
        }
    }

    /**
     * Generate a RRA line for rrdtool
     */
    private String getRraStr(ArcDef def) {
        return "RRA:" + def.getConsolFun() + ":" + def.getXff() + ":" + def.getSteps() + ":" + def.getRows();
    }

    /**
    "rrdtool create temperature.rrd --step 300 \\\n" + 
    "         DS:temp:GAUGE:600:-273:5000 \\\n" + 
    "         RRA:AVERAGE:0.5:1:1200 \\\n" + 
    "         RRA:MIN:0.5:12:2400 \\\n" + 
    "         RRA:MAX:0.5:12:2400 \\\n" + 
    "         RRA:AVERAGE:0.5:12:2400"
     */
    private String getDsDefStr(DsDef def) {
        return "DS:" + def.getDsName() + ":" + def.getDsType() + ":" + def.getHeartbeat() + ":" + formatDouble(def.getMinValue()) + ":" + formatDouble(def.getMaxValue());
    }

    /**
     * Get a list of DsNames used to create the datasource.
     */
    private List<String> getDsNames(DsDef[] defs) {
        List<String> names = new ArrayList<String>();
        for (DsDef def : defs) {
            names.add(def.getDsName());
        }
        return names;
    }

    /**
     * If dbl is NaN, then return U
     */
    private String formatDouble(double dbl) {
        if (Double.isNaN(dbl)) {
            return "U";
        }
        return String.valueOf(dbl);
    }
}
