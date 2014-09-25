package com.googlecode.jmxtrans.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

/**
 * This is the container for a list of Servers.
 * 
 * @author jon
 */
@JsonSerialize(include = NON_NULL)
public class JmxProcess {

	private String name;
	private List<Server> servers = new ArrayList<Server>();
	private Integer numMultiThreadedServers;

	public JmxProcess() {
	}

	public JmxProcess(Server server) {
		this.servers.add(server);
	}

	public JmxProcess(List<Server> servers) {
		this.setServers(servers);
	}

	public void setServers(List<Server> servers) {
		this.servers = servers;
	}

	public List<Server> getServers() {
		return servers;
	}

	@JsonIgnore
	public boolean isServersMultiThreaded() {
		return this.numMultiThreadedServers != null && this.numMultiThreadedServers > 0;
	}

	/**
	 * Set this if you want each JmxProcess to run in its own thread up to the
	 * count you set. So, if you set this to 3 and you have 6 JmxProcesses,
	 * it'll start up a max of 3 threads and then block until a thread becomes
	 * available. It'll then start another thread up to fill in the blanks.
	 */
	public void setNumMultiThreadedServers(Integer numMultiThreadedServers) {
		this.numMultiThreadedServers = numMultiThreadedServers;
	}

	public Integer getNumMultiThreadedServers() {
		return numMultiThreadedServers;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
