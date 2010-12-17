package com.googlecode.jmxtrans.util;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.jmxtrans.OutputWriter;

/**
 * Implements the common code for output filters.
 * 
 * @author jon
 */
public abstract class BaseOutputWriter implements OutputWriter {

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String OUTPUT_FILE = "outputFile";
    public static final String TEMPLATE_FILE = "templateFile";
    public static final String BINARY_PATH = "binaryPath";

    private Map<String, Object> settings;

    public void addSetting(String key, Object value) {
        getSettings().put(key, value);
    }

    public Map<String, Object> getSettings() {
        if (this.settings == null) {
            this.settings = new HashMap<String, Object>();
        }
        return this.settings;
    }
    
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
}
