package com.googlecode.jmxtrans.model.output;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdDefTemplate;
import org.jrobin.core.Sample;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;

/**
 * This takes a JRobin template.xml file and then creates the database if it doesn't already exist.
 * 
 * It will then write the contents of the Query (the Results) to the database.
 * 
 * This uses the JRobin rrd format and is incompatible with the C version of rrd.
 * 
 * @author jon
 */
public class RRDWriter extends BaseOutputWriter {

    /** */
    public RRDWriter() {
    }

    public void validateSetup() throws Exception {
        if (this.getOutputFile() == null || this.getTemplateFile() == null) {
            throw new RuntimeException("output file and template file can't be null");
        }
    }

    /** */
    public void doWrite(Query query) throws Exception {
        RrdDb db = null;
        try {
            db = createOrOpenDatabase();
            Sample sample = db.createSample();
            List<String> dsNames = Arrays.asList(db.getDsNames());
            List<Result> results = query.getResults();
            
            // go over all the results and look for datasource names that map to keys from the result values
            for (Result res : results) {
                Map<String, Object> values = res.getValues();
                for (Entry<String, Object> entry : values.entrySet()) {
                    if (dsNames.contains(entry.getKey())) {
                        sample.setValue(entry.getKey(), Double.valueOf(entry.getValue().toString()));
                    }
                }
            }
            sample.update();
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * If the database file doesn't exist, it'll get created, otherwise, it'll be returned in r/w mode.
     */
    protected RrdDb createOrOpenDatabase() throws Exception {
        RrdDb result = null;
        if (!this.getOutputFile().exists()) {
            FileUtils.forceMkdir(this.getOutputFile().getParentFile());
            RrdDefTemplate t = new RrdDefTemplate(this.getTemplateFile());
            t.setVariable("database", this.getOutputFile().getCanonicalPath());
            RrdDef def = t.getRrdDef();
            result = new RrdDb(def);
        } else {
            result = new RrdDb(this.getOutputFile().getCanonicalPath());
        }
        return result;
    }
}
