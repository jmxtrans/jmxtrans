package com.googlecode.jmxtrans.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.util.ValidationException;

/**
 * Represents a jmx server that we want to connect to.
 * This also stores the queries that we want to execute against the server.
 * @author jon
 */
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonPropertyOrder(value={"alias", "host", "port", "username", "password", "cronExpression", "numQueryThreads"})
public class Server {

	private static final String FRONT = "service:jmx:rmi:///jndi/rmi://";
    private static final String BACK = "/jmxrmi";
    public static final String SOCKET_FACTORY_POOL = "SocketFactory";

    private JmxProcess jmxProcess;

    private String alias;
    private String host;
    private String port;
    private String username;
    private String password;
    private String url;
    private String cronExpression;
    private Integer numQueryThreads;

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
        return jmxProcess;
    }

    /**
     * Some writers (GraphiteWriter) use the alias in generation of the unique key which references
     * this server.
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Some writers (GraphiteWriter) use the alias in generation of the unique key which references
     * this server.
     */
    public String getAlias() {
        return alias;
    }

    /** */
    public void setHost(String host) {
        this.host = host;
    }

    /** */
    public String getHost() {
        return host;
    }

    /** */
    public void setPort(String port) {
        this.port = port;
    }

    /** */
    public String getPort() {
        return port;
    }

    /** */
    public void setUsername(String username) {
        this.username = username;
    }

    /** */
    public String getUsername() {
        return username;
    }

    /** */
    public void setPassword(String password) {
        this.password = password;
    }

    /** */
    public String getPassword() {
        return password;
    }

    /** */
    public void setQueries(List<Query> queries) throws ValidationException {
        validateSetup(queries);
        this.queries = queries;
    }

    /** */
    public List<Query> getQueries() {
        return queries;
    }

    /**
     * Adds a query. Won't add the same query (determined by equals()) 2x.
     */
    public void addQuery(Query q) throws ValidationException {
        validateSetup(q);
        if (!this.queries.contains(q)) {
        	this.queries.add(q);
        }
    }

    /** */
    private void validateSetup(List<Query> queries) throws ValidationException {
        for (Query q : queries) {
            validateSetup(q);
        }
    }

    /** */
    private void validateSetup(Query query) throws ValidationException {
        List<OutputWriter> writers = query.getOutputWriters();
        if (writers != null) {
            for (OutputWriter w : writers) {
                w.validateSetup();
            }
        }
    }

    /**
     * The jmx url to connect to. Builds this from host/port.
     */
    @JsonIgnore
    public String getUrl() {
        if (this.host == null || this.port == null) {
            throw new RuntimeException("host or port is null");
        }

        if (this.url == null) {
            this.url = FRONT + host + ":" + port + BACK;
        }
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
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
        return this.numQueryThreads != null && this.numQueryThreads > 0;
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
        return numQueryThreads;
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
        return cronExpression;
    }

    /** */
    @Override
    public String toString() {
        return "Server [host=" + host + ", port=" + port + ", url=" + url + ", cronExpression="
                + cronExpression + ", queries=" + queries + ", numQueryThreads=" + numQueryThreads + "]";
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
}
