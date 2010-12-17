package com.googlecode.jmxtrans;

import java.util.Map;

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
    
    /**
     * Settings allow you to configure your Writers with whatever they might need.
     */
    public Map<String, Object> getSettings();

    /**
     * Settings allow you to configure your Writers with whatever they might need.
     */
    public void setSettings(Map<String, Object> settings);
    
    /**
     * This is run when the object is instantiated. You want to get the settings and validate them.
     */
    public void validateSetup() throws Exception;
}
