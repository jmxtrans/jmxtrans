package com.googlecode.jmxtrans.cli;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;

public class JmxTransConfiguration {
	private boolean continueOnJsonError = false;
	private File jsonDirOrFile;
	private boolean runEndlessly = false;
	/**
	 * The Quartz server properties.
	 */
	private String quartPropertiesFile = null;
	/**
	 * The seconds between server job runs.
	 */
	private int runPeriod = 60;
	private boolean help = false;

	private List<File> additionalJars = ImmutableList.of();

	/**
	 * If it is false, then JmxTrans will stop when one of the JSON
	 * configuration file is invalid. Otherwise, it will just print an error
	 * and continue processing.
	 *
	 * @param continueOnJsonError
	 */
	public void setContinueOnJsonError(boolean continueOnJsonError) {
		this.continueOnJsonError = continueOnJsonError;
	}

	/**
	 * Sets the json dir or file.
	 *
	 * @param jsonDirOrFile the json dir or file
	 */
	public void setJsonDirOrFile(File jsonDirOrFile) {
		this.jsonDirOrFile = jsonDirOrFile;
	}

	/**
	 * If this is true, then this class will execute the main() loop and then
	 * wait 60 seconds until running again.
	 *
	 * @param runEndlessly
	 */
	public void setRunEndlessly(boolean runEndlessly) {
		this.runEndlessly = runEndlessly;
	}

	/**
	 * Sets the quart properties file.
	 *
	 * @param quartPropertiesFile the quart properties file
	 */
	public void setQuartPropertiesFile(String quartPropertiesFile) {
		this.quartPropertiesFile = quartPropertiesFile;
	}

	/**
	 * Sets the run period.
	 *
	 * @param runPeriod the run period
	 */
	public void setRunPeriod(int runPeriod) {
		this.runPeriod = runPeriod;
	}

	/**
	 * Gets the json dir or file.
	 *
	 * @return the json dir or file
	 */
	public File getJsonDirOrFile() {
		return jsonDirOrFile;
	}

	public boolean isRunEndlessly() {
		return runEndlessly;
	}

	public boolean isContinueOnJsonError() {
		return continueOnJsonError;
	}

	/**
	 * Gets the quart properties file.
	 *
	 * @return the quart properties file
	 */
	public String getQuartPropertiesFile() {
		return quartPropertiesFile;
	}

	/**
	 * Gets the run period.
	 *
	 * @return the number of seconds between two runs
	 */
	public int getRunPeriod() {
		return runPeriod;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}

	public boolean isHelp() {
		return help;
	}

	public List<File> getAdditionalJars() {
		return additionalJars;
	}

	public void setAdditionalJars(List<File> additionalJars) {
		this.additionalJars = additionalJars;
	}
}
