package com.googlecode.jmxtrans.model;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.util.DatagramSocketFactory;
import com.googlecode.jmxtrans.util.JmxConnectionFactory;
import com.googlecode.jmxtrans.util.PropertyResolver;
import com.googlecode.jmxtrans.util.SocketFactory;
import com.googlecode.jmxtrans.util.ValidationException;

import javax.management.MBeanServer;

/**
 * Represents a jmx server that we want to connect to.
 * This also stores the queries that we want to execute against the server.
 * @author jon
 */
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonPropertyOrder(value={"alias", "local", "host", "port", "username", "password", "cronExpression", "numQueryThreads", "protocolProviderPackages"})
public class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	private static final String FRONT = "service:jmx:rmi:///jndi/rmi://";
	private static final String BACK = "/jmxrmi";
	public static final String SOCKET_FACTORY_POOL = SocketFactory.class.getSimpleName();
	public static final String JMX_CONNECTION_FACTORY_POOL = JmxConnectionFactory.class.getSimpleName();
	public static final String DATAGRAM_SOCKET_FACTORY_POOL = DatagramSocketFactory.class.getSimpleName();

	private JmxProcess jmxProcess;

	private String alias;
	private String host;
	private String port;
	private String username;
	private String password;
	private String protocolProviderPackages;
	private String url;
	private String cronExpression;
	private Integer numQueryThreads;

    // if using local JMX to embed JmxTrans to query the local MBeanServer
    private boolean local;
    private MBeanServer localMBeanServer;

	private List<Query> queries = new ArrayList<Query>();

    public Server() { }

	/** */
	public Server(String host, String port) {
		this.host = host;
		this.port = port;
	}

	/** */
	public Server(String host, String port, Query query) throws ValidationException {
		this.host = host;
		this.port = port;
		this.addQuery(query);
	}

	/**
	 * The parent container in json
	 */
	public void setJmxProcess(JmxProcess jmxProcess) {
		this.jmxProcess = jmxProcess;
	}

	/**
	 * The parent container in json
	 */
	@JsonIgnore
	public JmxProcess getJmxProcess() {
		return this.jmxProcess;
	}

    @JsonIgnore
    public MBeanServer getLocalMBeanServer() {
        if (localMBeanServer == null) {
            localMBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return localMBeanServer;
    }

    public void setLocalMBeanServer(MBeanServer localMBeanServer) {
        this.localMBeanServer = localMBeanServer;
    }


	/**
	 * Some writers (GraphiteWriter) use the alias in generation of the unique key which references
	 * this server.
	 */
	public void setAlias(String alias) {
		this.alias = PropertyResolver.resolveProps(alias);
	}

	/**
	 * Some writers (GraphiteWriter) use the alias in generation of the unique key which references
	 * this server.
	 */
	public String getAlias() {
		return this.alias;
	}

	/** */
	public void setHost(String host) {
		this.host = PropertyResolver.resolveProps(host);
	}

	/** */
	public String getHost() {
		return this.host;
	}

	/** */
	public void setPort(String port) {
		this.port = PropertyResolver.resolveProps(port);
	}

	/** */
	public String getPort() {
		return this.port;
	}

	/** */
	public void setUsername(String username) {
		this.username = PropertyResolver.resolveProps(username);
	}

	/** */
	public String getUsername() {
		return this.username;
	}

	/** */
	public void setPassword(String password) {
		this.password = PropertyResolver.resolveProps(password);
	}

	/** */
	public String getPassword() {
		return this.password;
	}

    /**
     * Whether the current local Java process should be used or not (useful for polling the embedded JVM when
     * using JmxTrans inside a JVM to poll JMX stats and push them remotely)
     */
    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    /**
	 * Won't add the same query (determined by equals()) 2x.
	 */
	public void setQueries(List<Query> queries) throws ValidationException {
		for (Query q : queries) {
			this.addQuery(q);
		}
	}

	/** */
	public List<Query> getQueries() {
		return this.queries;
	}

	/**
	 * Adds a query. Won't add the same query (determined by equals()) 2x.
	 */
	public void addQuery(Query q) throws ValidationException {
		if (!this.queries.contains(q)) {
			this.queries.add(q);
		} else {
			log.debug("Skipped duplicate query: " + q + " for server: " + this);
		}
	}

	/**
	 * The jmx url to connect to. If null, it builds this from host/port with a standard configuration. Other
	 * JVM's may want to set this value.
	 */
	@JsonIgnore
	public String getUrl() {
		if (this.url == null) {
			if ((this.host == null) || (this.port == null)) {
				throw new RuntimeException("url is null and host or port is null. cannot construct url dynamically.");
			}
			this.url = FRONT + this.host + ":" + this.port + BACK;
		}
		return this.url;
	}

	public void setUrl(String url) {
		this.url = PropertyResolver.resolveProps(url);
	}

	/**
	 * If there are queries and results that have been executed,
	 * this is just a shortcut to get all the Results.
	 *
	 * @return null if there are no queries or empty list if there are no results.
	 */
	@JsonIgnore
	public List<Result> getResults() {
		List<Query> queries = this.getQueries();
		List<Result> results = null;
		if (queries != null) {
			results = new ArrayList<Result>();
			for (Query q : queries) {
				List<Result> tmp = q.getResults();
				if (tmp != null) {
					results.addAll(tmp);
				}
			}
		}
		return results;
	}

	/** */
	@JsonIgnore
	public boolean isQueriesMultiThreaded() {
		return (this.numQueryThreads != null) && (this.numQueryThreads > 0);
	}

	/**
	 * The number of query threads for this server.
	 */
	public void setNumQueryThreads(Integer numQueryThreads) {
		this.numQueryThreads = numQueryThreads;
	}

	/**
	 * The number of query threads for this server.
	 */
	public Integer getNumQueryThreads() {
		return this.numQueryThreads;
	}

	/**
	 * Each server can set a cronExpression for the scheduler.
	 * If the cronExpression is null, then the job is run immediately
	 * and once. Otherwise, it is added to the scheduler for immediate
	 * execution and run according to the cronExpression.
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * Each server can set a cronExpression for the scheduler.
	 * If the cronExpression is null, then the job is run immediately
	 * and once. Otherwise, it is added to the scheduler for immediate
	 * execution and run according to the cronExpression.
	 */
	public String getCronExpression() {
		return this.cronExpression;
	}

	/** */
	@Override
	public String toString() {
		return "Server [host=" + this.host + ", port=" + this.port + ", url=" + this.url + ", cronExpression="
				+ this.cronExpression + ", numQueryThreads=" + this.numQueryThreads + "]";
	}

	/** */
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

		Server other = (Server)o;

		return new EqualsBuilder()
								.append(this.getHost(), other.getHost())
								.append(this.getPort(), other.getPort())
								.append(this.getNumQueryThreads(), other.getNumQueryThreads())
								.append(this.getCronExpression(), other.getCronExpression())
								.append(this.getAlias(), other.getAlias())
								.append(this.getUsername(), other.getUsername())
								.append(this.getPassword(), other.getPassword()).isEquals();
	}

	/** */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(13, 21)
										.append(this.getHost())
										.append(this.getPort())
										.append(this.getNumQueryThreads())
										.append(this.getCronExpression())
										.append(this.getAlias())
										.append(this.getUsername())
										.append(this.getPassword()).toHashCode();
	}

	/**
	 * This is some obtuse shit for enabling weblogic support.
	 *
	 * http://download.oracle.com/docs/cd/E13222_01/wls/docs90/jmx/accessWLS.html
	 *
	 * You'd set this to: weblogic.management.remote
	 */
	public String getProtocolProviderPackages() {
		return protocolProviderPackages;
	}

	/**
	 * This is some obtuse shit for enabling weblogic support.
	 *
	 * http://download.oracle.com/docs/cd/E13222_01/wls/docs90/jmx/accessWLS.html
	 *
	 * You'd set this to: weblogic.management.remote
	 */
	public void setProtocolProviderPackages(String protocolProviderPackages) {
		this.protocolProviderPackages = protocolProviderPackages;
	}
}
