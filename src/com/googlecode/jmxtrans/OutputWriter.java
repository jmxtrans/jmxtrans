package com.googlecode.jmxtrans;

import java.io.File;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.googlecode.jmxtrans.model.Query;

/**
 * Interface which defines a writer for taking jmx data and 
 * writing it out in whatever format you want.
 * 
 * Note that this class uses a feature of Jackson to serialize
 * anything that implements this as a "@class". That way, when
 * Jackson deserializes implementations of this interface, it is
 * done with new objects that implement this interface.
 * 
 * @author jon
 */
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface OutputWriter {

    public void doWrite(Query query) throws Exception;
    
    public File getTemplateFile();
    public void setTemplateFile(File template);
    
    public File getOutputFile();
    public void setOutputFile(File outputFile);

    public File getBinaryPath();
    public void setBinaryPath(File binaryPath);

    public String getHost();
    public void setHost(String host);

    public Integer getPort();
    public void setPort(Integer port);

    /**
     * This is run when the object is instantiated.
     */
    public void validateSetup() throws Exception;
}
