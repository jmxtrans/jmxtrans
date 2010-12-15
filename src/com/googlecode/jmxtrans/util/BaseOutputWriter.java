package com.googlecode.jmxtrans.util;

import java.io.File;

import com.googlecode.jmxtrans.OutputWriter;

/**
 * Implements the common code for output filters.
 * 
 * @author jon
 */
public abstract class BaseOutputWriter implements OutputWriter {

    private File outputFile;
    private File templateFile;
    private File binaryPath;
    
    private String host;
    private Integer port;
    
    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public File getTemplateFile() {
        return this.templateFile;
    }
    
    public void setTemplateFile(File templateFile) {
        this.templateFile = templateFile;
    }

    public void setBinaryPath(File binaryPath) {
        this.binaryPath = binaryPath;
    }

    public File getBinaryPath() {
        return binaryPath;
    }

    public String getHost() {
        return this.host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public Integer getPort() {
        return this.port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
}
