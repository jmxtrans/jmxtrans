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
}
