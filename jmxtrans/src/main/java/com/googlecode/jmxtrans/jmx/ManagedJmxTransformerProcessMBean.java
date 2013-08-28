package com.googlecode.jmxtrans.jmx;

import java.io.File;

import com.googlecode.jmxtrans.util.LifecycleException;

/**
 * The Interface ManagedJmxTransformerProcessMBean.
 */
public interface ManagedJmxTransformerProcessMBean {
	
	/**
	 * Start the JmxProcess.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	public abstract void start() throws LifecycleException;
	
	/**
	 * Stop the JmxProcess.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	public abstract void stop() throws LifecycleException;
	
	/**
	 * Gets the quart properties file.
	 *
	 * @return the quart properties file
	 */
	public String getQuartPropertiesFile();
	
	/**
	 * Sets the quart properties file.
	 *
	 * @param quartPropertiesFile the quart properties file
	 */
	public void setQuartPropertiesFile(String quartPropertiesFile);
	
	/**
	 * Gets the run period.
	 *
	 * @return the run period
	 */
	public int getRunPeriod();
	
	/**
	 * Sets the run period.
	 *
	 * @param runPeriod the run period
	 */
	public void setRunPeriod(int runPeriod);
	
	/**
	 * Sets the json dir or file.
	 *
	 * @param jsonDirOrFile the json dir or file
	 */
	public void setJsonDirOrFile(File jsonDirOrFile);
	
	/**
	 * Gets the json dir or file.
	 *
	 * @return the json dir or file
	 */
	public File getJsonDirOrFile();
}