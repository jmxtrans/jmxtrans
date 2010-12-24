package com.googlecode.jmxtrans.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.annotate.JsonIgnore;

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
    public static final String DEBUG = "debug";
    public static final String TYPE_NAMES = "typeNames";

    private Boolean debugEnabled = null;
    private Map<String, Object> settings;

    /** */
    public void addSetting(String key, Object value) {
        getSettings().put(key, value);
    }

    /** */
    public Map<String, Object> getSettings() {
        if (this.settings == null) {
            this.settings = new TreeMap<String, Object>();
        }
        return this.settings;
    }
    
    /** */
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
    
    /** */
    public boolean getBooleanSetting(String key) {
        return this.getSettings().containsKey(key) ? (Boolean) this.getSettings().get(key) : Boolean.FALSE;
    }

    /** */
    @JsonIgnore
    public boolean isDebugEnabled() {
        if (debugEnabled == null) {
            debugEnabled =  this.getSettings().containsKey(DEBUG) ? (Boolean) this.getSettings().get(DEBUG) : Boolean.FALSE;
        }
        return debugEnabled != null ? debugEnabled : false;
    }

    /** */
    public void setTypeNames(List<String> typeNames) {
        this.getSettings().put(TYPE_NAMES, typeNames);
    }

    /** */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<String> getTypeNames() {
        if (!this.getSettings().containsKey(TYPE_NAMES)) {
            List<String> tmp = new ArrayList<String>();
            this.getSettings().put(TYPE_NAMES, tmp);
        }
        return (List<String>) this.getSettings().get(TYPE_NAMES);
    }

    /** */
    public void addTypeName(String str) {
        this.getTypeNames().add(str);
    }

    /**
     * Given a typeName string, get the first match from the typeNames setting.
     * In other words, suppose you have:
     * 
     * typeName=name=PS Eden Space,type=MemoryPool
     * 
     * If you addTypeName("name"), then it'll retrieve 'PS Eden Space' from the string
     */
    protected String retrieveTypeNameValue(String typeNameStr) {
        List<String> typeNames = getTypeNames();
        if (typeNames == null || typeNames.size() == 0) {
            return null;
        }
        String[] tokens = typeNameStr.split(",");
        boolean foundIt = false;
        for (String token : tokens) {
            String[] keys = token.split("=");
            for (String key : keys) {
                // we want the next item in the array.
                if (foundIt) {
                    return key;
                }
                if (typeNames.contains(key)) {
                    foundIt = true;
                }
            }
        }
        return null;
    }

    /**
     * Replaces all . with _ and removes all spaces.
     */
    protected String cleanupStr(String name) {
        if (name == null) {
            return null;
        }
        String clean = name.replace('.', '_');
        clean = clean.replace(" ", "");
        return clean;
    }

}
