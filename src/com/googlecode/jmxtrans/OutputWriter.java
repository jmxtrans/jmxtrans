package com.googlecode.jmxtrans;

import java.util.Map;

import org.apache.commons.pool.KeyedObjectPool;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * Interface which defines a writer for taking jmx data and writing it out in
 * whatever format you want.
 * 
 * Note that this class uses a feature of Jackson to serialize anything that
 * implements this as a "@class". That way, when Jackson deserializes
 * implementations of this interface, it is done with new objects that implement
 * this interface.
 * 
 * @author jon
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface OutputWriter {

	public void start() throws LifecycleException;

	public void stop() throws LifecycleException;

	public void doWrite(Query query) throws Exception;

	/**
	 * Settings allow you to configure your Writers with whatever they might
	 * need.
	 */
	public Map<String, Object> getSettings();

	/**
	 * Settings allow you to configure your Writers with whatever they might
	 * need.
	 */
	public void setSettings(Map<String, Object> settings);

	/**
	 * This is run when the object is instantiated. You want to get the settings
	 * and validate them.
	 */
	public void validateSetup(Query query) throws ValidationException;

	/**
	 * Some writers, like GraphiteWriter will use this for object pooling.
	 * Things like Socket connections to remote servers that we are writing to
	 * are ripe for pooling.
	 * 
	 * This is super extensible as a map because we could have multiple object
	 * pools.
	 */
	public void setObjectPoolMap(Map<String, KeyedObjectPool> poolMap);
}
