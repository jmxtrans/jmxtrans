package com.googlecode.jmxtrans.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;

import java.util.Map;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

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
@JsonSerialize(include = NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface OutputWriter {

	void start() throws LifecycleException;

	void stop() throws LifecycleException;

	void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception;

	/**
	 * Settings allow you to configure your Writers with whatever they might
	 * need.
	 */
	Map<String, Object> getSettings();

	/**
	 * Settings allow you to configure your Writers with whatever they might
	 * need.
	 */
	void setSettings(Map<String, Object> settings);

	/**
	 * This is run when the object is instantiated. You want to get the settings
	 * and validate them.
	 */
	void validateSetup(Server server, Query query) throws ValidationException;

}
