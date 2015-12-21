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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.connections.JMXConnectionParams;
import com.sun.tools.attach.VirtualMachine;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.LifecycledPool;
import stormpot.TimeSpreadExpiration;
import stormpot.Timeout;

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
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveProps;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
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
@EqualsAndHashCode(exclude = {"queries", "pool"})
@ToString(of = {"pid", "host", "port", "url", "cronExpression", "numQueryThreads"})
public class Server implements LifecycleAware {

	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final String FRONT = "service:jmx:rmi:///jndi/rmi://";
	private static final String BACK = "/jmxrmi";

	/**
	 * Some writers (GraphiteWriter) use the alias in generation of the unique
	 * key which references this server.
	 */
	@Getter private final String alias;

	/** Returns the pid of the local process jmxtrans will attach to. */
	@Getter private final String pid;
	private final String host;
	private final String port;
	@Getter private final String username;
	@Getter private final String password;
	/**
	 * This is some obtuse shit for enabling weblogic support.
	 * <p/>
	 * http://download.oracle.com/docs/cd/E13222_01/wls/docs90/jmx/accessWLS.
	 * html
	 * <p/>
	 * You'd set this to: weblogic.management.remote
	 */
	@Getter private final String protocolProviderPackages;
	private final String url;
	/**
	 * Each server can set a cronExpression for the scheduler. If the
	 * cronExpression is null, then the job is run immediately and once.
	 * Otherwise, it is added to the scheduler for immediate execution and run
	 * according to the cronExpression.
	 */
	@Getter private final String cronExpression;
	/** The number of query threads for this server. */
	@Getter private final int numQueryThreads;

	/**
	 * Whether the current local Java process should be used or not (useful for
	 * polling the embedded JVM when using JmxTrans inside a JVM to poll JMX
	 * stats and push them remotely)
	 */
	@Getter private final boolean local;

	@Getter private final ImmutableSet<Query> queries;

	private final LifecycledPool<MBeanServerConnectionPoolable> pool;

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

		checkArgument(pid != null || url != null || host != null,
				"You must provide the pid or the [url|host and port]");
		checkArgument(!(pid != null && (url != null || host != null)),
				"You must provide the pid OR the url, not both");

		this.alias = resolveProps(alias);
		this.pid = resolveProps(pid);
		this.port = resolveProps(port);
		this.username = resolveProps(username);
		this.password = resolveProps(password);
		this.protocolProviderPackages = protocolProviderPackages;
		this.url = resolveProps(url);
		this.cronExpression = cronExpression;
		this.numQueryThreads = firstNonNull(numQueryThreads, 0);
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
		pool = createPool(this.numQueryThreads);
	}

	private LifecycledPool<MBeanServerConnectionPoolable> createPool(int numQueryThreads) {
		Config<MBeanServerConnectionPoolable> config = new Config<MBeanServerConnectionPoolable>()
				.setAllocator(new MBeanServerConnectionAllocator(this))
				.setExpiration(new TimeSpreadExpiration<MBeanServerConnectionPoolable>(30, 60, MINUTES))
				.setSize(max(numQueryThreads, 1));
		return new BlazePool<MBeanServerConnectionPoolable>(config);
	}

	public void executeJmx(JmxAction call, Timeout timeout) throws Exception {
		MBeanServerConnectionPoolable poolable = pool.claim(timeout);
		try {
			call.execute(poolable.getConnection());
		} catch (Exception ioe) {
			poolable.invalidate();
			throw ioe;
		} finally {
			poolable.release();
		}
	}

	public <T> T callJmx(JmxCall action, Timeout timeout) throws Exception {
		MBeanServerConnectionPoolable poolable = pool.claim(timeout);
		try {
			return action.execute(poolable.getConnection());
		} catch (Exception ioe) {
			poolable.invalidate();
			throw ioe;
		} finally {
			poolable.release();
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

	public String getSource() {
		if (alias != null) {
			return alias;
		}

		return this.getHost();
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
		return numQueryThreads > 0;
	}

	@JsonIgnore
	public JMXConnectionParams getJmxConnectionParams() throws IOException {
		return new JMXConnectionParams(getJmxServiceURL(), getEnvironment());
	}

	@Override
	public void shutdown() {
		pool.shutdown();
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
	@Accessors(chain = true)
	public static final class Builder {
		@Setter private String alias;
		@Setter private String pid;
		@Setter private String host;
		@Setter private String port;
		@Setter private String username;
		@Setter private String password;
		@Setter private String protocolProviderPackages;
		@Setter private String url;
		@Setter private String cronExpression;
		@Setter private Integer numQueryThreads;
		@Setter private boolean local;
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
