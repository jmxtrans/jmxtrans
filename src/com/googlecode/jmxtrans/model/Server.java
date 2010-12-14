package com.googlecode.jmxtrans.model;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.googlecode.jmxtrans.OutputWriter;

/**
 * Represents a jmx server that we want to connect to.
 * This also stores the queries that we want to execute against the server.
 * @author jon
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class Server {
    private Integer numQueryThreads;

    private static final String FRONT = "service:jmx:rmi:///jndi/rmi://";
    private static final String BACK = "/jmxrmi";
    private String host;
    private String port;
    private String username;
    private String password;
    private String url;

    private List<Query> queries = new ArrayList<Query>();
    
    public Server() { }

    public Server(String host, String port) {
        this.host = host;
        this.port = port;
    }
    
    public Server(String host, String port, Query query) throws Exception {
        this.host = host;
        this.port = port;
        this.addQuery(query);
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setQueries(List<Query> queries) throws Exception {
        validateSetup(queries);
        this.queries = queries;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public void addQuery(Query q) throws Exception {
        validateSetup(q);
        this.queries.add(q);
    }

    private void validateSetup(List<Query> queries) throws Exception {
        for (Query q : queries) {
            validateSetup(q);
        }
    }

    private void validateSetup(Query query) throws Exception {
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
    
    public Integer getNumQueryThreads() {
        return numQueryThreads;
    }
}
