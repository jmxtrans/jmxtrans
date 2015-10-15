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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;
import static java.util.Arrays.asList;
import static javax.management.remote.JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;

/**
 * Represents a jmx server that we want to connect to. This also stores the
 * queries that we want to execute against the server.
 *
 * @author jon
 */
@JsonSerialize(include = NON_NULL)
@JsonPropertyOrder(value = {
		"alias",
		"local",
		"pid",
		"host",
		"port",
		"username",
		"password",
		"cronExpression",
		"numQueryThreads",
		"protocolProviderPackages"
})
@Immutable
@ThreadSafe
public class Server {

	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final String FRONT = "service:jmx:rmi:///jndi/rmi://";
	private static final String BACK = "/jmxrmi";

	private final String alias;
	private final String pid;
	private final String host;
	private final String port;
	private final String username;
	private final String password;
	private final String protocolProviderPackages;
	private final String url;
	private final String cronExpression;
	private final Integer numQueryThreads;

	// if using local JMX to embed JmxTrans to query the local MBeanServer
	private final boolean local;

	private final ImmutableSet<Query> queries;

	@JsonCreator
	public Server(
			@JsonProperty("alias") String alias,
			@JsonProperty("pid") String pid,
			@JsonProperty("host") String host,
			@JsonProperty("port") String port,
			@JsonProperty("username") String username,
			@JsonProperty("password") String password,
			@JsonProperty("protocolProviderPackages") String protocolProviderPackages,
			@JsonProperty("url") String url,
			@JsonProperty("cronExpression") String cronExpression,
			@JsonProperty("numQueryThreads") Integer numQueryThreads,
			@JsonProperty("local") boolean local,
			@JsonProperty("queries") List<Query> queries) {

		Preconditions.checkArgument(pid != null || url != null || host != null,
			"You must provide the pid or the [url|host and port]");
		Preconditions.checkArgument(!(pid != null && (url != null || host != null)),
			"You must provide the pid OR the url, not both");

		this.alias = resolveProps(alias);
		this.pid = resolveProps(pid);
		this.port = resolveProps(port);
		this.username = resolveProps(username);
		this.password = resolveProps(password);
		this.protocolProviderPackages = protocolProviderPackages;
		this.url = resolveProps(url);
		this.cronExpression = cronExpression;
		this.numQueryThreads = numQueryThreads;
		this.local = local;
		this.queries = copyOf(queries);

		// when connecting in local, we cache the host after retrieving it from the network card
		if(pid != null) {
			try {
				this.host = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				// should work, so just throw a runtime if it doesn't
				throw new RuntimeException(e);
			}
		}
		else {
			this.host = resolveProps(host);
		}
	}

	/**
	 * Generates the proper username/password environment for JMX connections.
	 */
	@JsonIgnore
	public ImmutableMap<String, ?> getEnvironment() {
		if (getProtocolProviderPackages() != null && getProtocolProviderPackages().contains("weblogic")) {
			ImmutableMap.Builder<String, String> environment = ImmutableMap.builder();
			if ((username != null) && (password != null)) {
				environment.put(PROTOCOL_PROVIDER_PACKAGES, getProtocolProviderPackages());
				environment.put(SECURITY_PRINCIPAL, username);
				environment.put(SECURITY_CREDENTIALS, password);
			}
			return environment.build();
		}

		ImmutableMap.Builder<String, String[]> environment = ImmutableMap.builder();
		if ((username != null) && (password != null)) {
			String[] credentials = new String[] {
					username,
					password
			};
			environment.put(JMXConnector.CREDENTIALS, credentials);
		}

		return environment.build();
	}

	/**
	 * Helper method for connecting to a Server. You need to close the resulting
	 * connection.
	 */
	@JsonIgnore
	public JMXConnector getServerConnection() throws Exception {
		JMXServiceURL url = new JMXServiceURL(getUrl());
		return JMXConnectorFactory.connect(url, this.getEnvironment());
	}

	@JsonIgnore
	public MBeanServer getLocalMBeanServer() {
		// Getting the platform MBean server is cheap (expect for th first call) no need to cache it.
		return ManagementFactory.getPlatformMBeanServer();
	}

	/**
	 * Some writers (GraphiteWriter) use the alias in generation of the unique
	 * key which references this server.
	 */
	public String getAlias() {
		return this.alias;
	}

	/**
	 * Returns the pid of the local process jmxtrans will attach to
	 *
	 * @return pid of the java process
	 */
	public String getPid() {
		return this.pid;
	}

	public String getHost() {
		if (host == null && url == null) {
			return null;
		}

		if (host != null) {
			return host;
		}

		// removed the caching of the extracted host as it is a very minor
		// optimization we should probably pre compute it in the builder and
		// throw exception at construction if both url and host are set
		// we might also be able to use java.net.URI to parse the URL, but I'm
		// not familiar enough with JMX URLs to think of the test cases ...
		return url.substring(url.lastIndexOf("//") + 2, url.lastIndexOf(':'));
	}

	public String getPort() {
		if (port == null && url == null) {
			return null;
		}
		if (this.port != null) {
			return port;
		}

		return extractPortFromUrl(url);
	}

	private static String extractPortFromUrl(String url) {
		String computedPort = url.substring(url.lastIndexOf(':') + 1);
		if (computedPort.contains("/")) {
			computedPort = computedPort.substring(0, computedPort.indexOf('/'));
		}
		return computedPort;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	/**
	 * Whether the current local Java process should be used or not (useful for
	 * polling the embedded JVM when using JmxTrans inside a JVM to poll JMX
	 * stats and push them remotely)
	 */
	public boolean isLocal() {
		return local;
	}

	public ImmutableSet<Query> getQueries() {
		return this.queries;
	}

	/**
	 * The jmx url to connect to. If null, it builds this from host/port with a
	 * standard configuration. Other JVM's may want to set this value.
	 */
	public String getUrl() {
		if (this.url == null) {
			if ((this.host == null) || (this.port == null)) {
				return null;
			}
			return FRONT + this.host + ":" + this.port + BACK;
		}
		return this.url;
	}

	@JsonIgnore
	public JMXServiceURL getJmxServiceURL() throws IOException {
		if(this.pid != null) {
			return JMXServiceURLFactory.extractJMXServiceURLFromPid(this.pid);
		}
		return new JMXServiceURL(getUrl());
	}

	@JsonIgnore
	public boolean isQueriesMultiThreaded() {
		return (this.numQueryThreads != null) && (this.numQueryThreads > 0);
	}

	/**
	 * The number of query threads for this server.
	 */
	public Integer getNumQueryThreads() {
		return this.numQueryThreads;
	}

	/**
	 * Each server can set a cronExpression for the scheduler. If the
	 * cronExpression is null, then the job is run immediately and once.
	 * Otherwise, it is added to the scheduler for immediate execution and run
	 * according to the cronExpression.
	 */
	public String getCronExpression() {
		return this.cronExpression;
	}

	@Override
	public String toString() {
		String msg;
		if(pid != null) {
			msg = "pid=" + pid;
		}
		else {
			msg = "host=" + this.host + ", port=" + this.port + ", url=" + this.url;
		}
		return "Server [" + msg + ", cronExpression=" + this.cronExpression
				+ ", numQueryThreads=" + this.numQueryThreads + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o.getClass() != this.getClass()) {
			return false;
		}

		if (!(o instanceof Server)) {
			return false;
		}

		Server other = (Server) o;

		return new EqualsBuilder()
				.append(this.getHost(), other.getHost())
				.append(this.getPort(), other.getPort())
				.append(this.getPid(), other.getPid())
				.append(this.getNumQueryThreads(), other.getNumQueryThreads())
				.append(this.getCronExpression(), other.getCronExpression())
				.append(this.getAlias(), other.getAlias())
				.append(this.getUsername(), other.getUsername())
				.append(this.getPassword(), other.getPassword())
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(13, 21)
				.append(this.getHost())
				.append(this.getPort())
				.append(this.getPid())
				.append(this.getNumQueryThreads())
				.append(this.getCronExpression())
				.append(this.getAlias())
				.append(this.getUsername())
				.append(this.getPassword())
				.toHashCode();
	}

	/**
	 * This is some obtuse shit for enabling weblogic support.
	 * <p/>
	 * http://download.oracle.com/docs/cd/E13222_01/wls/docs90/jmx/accessWLS.
	 * html
	 * <p/>
	 * You'd set this to: weblogic.management.remote
	 */
	public String getProtocolProviderPackages() {
		return protocolProviderPackages;
	}

	/**
	 * Factory to create a JMXServiceURL from a pid. Inner class to prevent class
	 * loader issues when tools.jar isn't present.
	 */
	private static class JMXServiceURLFactory {

		public static JMXServiceURL extractJMXServiceURLFromPid(String pid) throws IOException {

			try {
				VirtualMachine vm = VirtualMachine.attach(pid);

				try {
					String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);

					if (connectorAddress == null) {
						String agent = vm.getSystemProperties().getProperty("java.home") +
								File.separator + "lib" + File.separator + "management-agent.jar";
						vm.loadAgent(agent);

						connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
					}

					return new JMXServiceURL(connectorAddress);
				} finally {
					vm.detach();
				}
			}
			catch(Exception e) {
				throw new IOException(e);
			}
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(Server server) {
		return new Builder(server);
	}

	@NotThreadSafe
	public static final class Builder {
		private String alias;
		private String pid;
		private String host;
		private String port;
		private String username;
		private String password;
		private String protocolProviderPackages;
		private String url;
		private String cronExpression;
		private Integer numQueryThreads;
		private boolean local;
		private final List<Query> queries = new ArrayList<Query>();

		private Builder() {}

		private Builder(Server server) {
			this.alias = server.alias;
			this.pid = server.pid;
			this.host = (server.pid != null ? null : server.host); // let the host be deduced in the constructor
			this.port = server.port;
			this.username = server.username;
			this.password = server.password;
			this.protocolProviderPackages = server.protocolProviderPackages;
			this.url = server.url;
			this.cronExpression = server.cronExpression;
			this.numQueryThreads = server.numQueryThreads;
			this.local = server.local;
			this.queries.addAll(server.queries);
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setPid(String pid) {
			this.pid = pid;
			return this;
		}

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public Builder setPort(String port) {
			this.port = port;
			return this;
		}

		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setProtocolProviderPackages(String protocolProviderPackages) {
			this.protocolProviderPackages = protocolProviderPackages;
			return this;
		}

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}

		public Builder setCronExpression(String cronExpression) {
			this.cronExpression = cronExpression;
			return this;
		}

		public Builder setNumQueryThreads(Integer numQueryThreads) {
			this.numQueryThreads = numQueryThreads;
			return this;
		}

		public Builder setLocal(boolean local) {
			this.local = local;
			return this;
		}

		public Builder addQuery(Query query) {
			this.queries.add(query);
			return this;
		}

		public Builder addQueries(Query... queries) {
			this.queries.addAll(asList(queries));
			return this;
		}

		public Builder addQueries(Set<Query> queries) {
			this.queries.addAll(queries);
			return this;
		}

		public Server build() {
			return new Server(
					alias,
					pid,
					host,
					port,
					username,
					password,
					protocolProviderPackages,
					url,
					cronExpression,
					numQueryThreads,
					local,
					queries);
		}
	}

}
