/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
