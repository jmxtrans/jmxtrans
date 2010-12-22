package com.googlecode.jmxtrans.model.output;

import java.io.BufferedReader;
import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;

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

    private static final Logger log = LoggerFactory.getLogger(RRDToolWriter.class);

    private File outputFile = null;
    private File templateFile = null;
    private File binaryPath = null;
    
    /** */
    public RRDToolWriter() {
    }

    public void validateSetup() throws Exception {
        outputFile = new File((String) this.getSettings().get(OUTPUT_FILE));
        templateFile = new File((String) this.getSettings().get(TEMPLATE_FILE));
        binaryPath = new File((String) this.getSettings().get(BINARY_PATH));

        if (outputFile == null || templateFile == null || binaryPath == null) {
            throw new RuntimeException("output, template and binary path file can't be null");
        }
    }

    /**
     * rrd datasources must be less than 21 characters in length, so
     * work to strip out the names to make it shorter. Not ideal at all,
     * but works.
     */
    public static String getDataSourceName(String name) {
        String newname = name.replace(" ", "");
        newname = newname.replace(".", "");
        newname = newname.replace("Space", "");
        newname = newname.replace("Info", "");
        newname = newname.replace("Usage", "");
        newname = abbreviateMiddle(newname, 20);
        return newname;
    }

    /**
     * Chops out characters in the middle of the string so that
     * the overall length is length. If str is <= length, it just
     * returns str. I had to write my own method cause commons-lang
     * StringUtils doesn't have a method like this.
     */
    public static String abbreviateMiddle(String str, int length) {
        if (str.length() <= length) {
            return str;
        }
        int targetSting = length;
        
        int startOffset = targetSting / 2 + targetSting % 2;
        int endOffset = str.length() - targetSting / 2;
        
        StringBuilder builder = new StringBuilder(length);
        builder.append(str.substring(0,startOffset));
        builder.append(str.substring(endOffset));

        return builder.toString();
    }
    
    /**
     * If an attribute has a . in the name, just return everything
     * after the last dot.
     */
    public static String getShortAttributeName(String attrName) {
        int index = attrName.lastIndexOf('.');
        if (index < 0) {
            return attrName;
        }
        return attrName.substring(index);
    }

    /** */
    public void doWrite(Query query) throws Exception {
        RrdDef def = getDatabaseTemplateSpec();

        List<String> dsNames = getDsNames(def.getDsDefs());
        List<Result> results = query.getResults();

        Map<String, String> dataMap = new TreeMap<String, String>();

        // go over all the results and look for datasource names that map to keys from the result values
        for (Result res : results) {
            log.debug(res.toString());
            Map<String, Object> values = res.getValues();
            if (values != null) {
                for (Entry<String, Object> entry : values.entrySet()) {
                    String shortAttrName = getShortAttributeName(res.getAttributeName());
                    String keyStr = StringUtils.capitalize(entry.getKey());
                    
                    String key = null;
                    if (keyStr.startsWith(shortAttrName)) {
                        key = getDataSourceName(keyStr);
                    } else {
                        key = getDataSourceName(shortAttrName + keyStr);
                    }
                    
                    boolean isNumeric = JmxUtils.isNumeric(entry.getValue());

                    if (isDebugEnabled() && isNumeric) {
                        log.debug("Generated DataSource name:value: " + key + " : " + entry.getValue());
                    }

                    if (dsNames.contains(key) && isNumeric) {
                        dataMap.put(key, entry.getValue().toString());
                    }
                }
            }
        }
        
        if (dataMap.keySet().size() > 0 && dataMap.values().size() > 0) {
            rrdToolUpdate(StringUtils.join(dataMap.keySet(), ':'), StringUtils.join(dataMap.values(), ':'));
        } else {
            log.error("Nothing was logged for query: " + query);
        }
    }

    /**
     * Executes the rrdtool update command.
     */
    protected void rrdToolUpdate(String template, String data) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(binaryPath + "/rrdtool");
        commands.add("update");
        commands.add(outputFile.getCanonicalPath());
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
        RrdDefTemplate t = new RrdDefTemplate(templateFile);
        t.setVariable("database", this.outputFile.getCanonicalPath());
        RrdDef def = t.getRrdDef();
        if (!this.outputFile.exists()) {
            FileUtils.forceMkdir(this.outputFile.getParentFile());
            rrdToolCreateDatabase(def);
        }
        return def;
    }
    
    /**
     * Calls out to the rrdtool binary with the 'create' command.
     */
    protected void rrdToolCreateDatabase(RrdDef def) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(this.binaryPath + "/rrdtool");
        commands.add("create");
        commands.add(this.outputFile.getCanonicalPath());
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
