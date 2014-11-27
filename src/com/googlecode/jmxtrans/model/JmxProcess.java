package com.googlecode.jmxtrans.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;

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
	private ImmutableList<Server> servers;
	private Integer numMultiThreadedServers;

	public JmxProcess() {
		this(ImmutableList.<Server>of());
	}

	public JmxProcess(Server server) {
		this.servers = ImmutableList.of(server);
	}

	public JmxProcess(ImmutableList<Server> servers) {
		this.setServers(servers);
	}

	public void setServers(List<Server> servers) {
		this.servers = ImmutableList.copyOf(servers);
	}

	public ImmutableList<Server> getServers() {
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
