package com.googlecode.jmxtrans.model;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.googlecode.jmxtrans.OutputWriter;

/**
 * Represents a JMX Query to ask for obj, attr and one or more keys.
 * 
 * Once the query has been executed, it'll have a list of results.
 * 
 * @author jon
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class Query {
    private Server server;
    
    private List<OutputWriter> outputWriters;

    private String obj;
    private List<String> attr;
    private List<Result> results;
    private List<String> keys;

    public Query() { }

    public Query(String obj) {
        this.obj = obj;
    }

    public Query(String obj, String attr) {
        this.obj = obj;
        addAttr(attr);
    }

    public Query(String obj, List<String> attr) {
        this.obj = obj;
        this.attr = attr;
    }

    public void setObj(String obj) {
        this.obj = obj;
    }

    public String getObj() {
        return obj;
    }

    public void setAttr(List<String> attr) {
        this.attr = attr;
    }

    public List<String> getAttr() {
        return attr;
    }

    public void addAttr(String attr) {
        if (this.attr == null) {
            this.attr = new ArrayList<String>();
        }
        this.attr.add(attr);
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void addKey(String key) {
        if (this.keys == null) {
            this.keys = new ArrayList<String>();
        }
        this.keys.add(key);
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    /**
     * We don't want Jackson to serialize the results if they exist.
     */
    @JsonIgnore
    public List<Result> getResults() {
        return results;
    }

    public void setOutputWriters(List<OutputWriter> outputWriters) {
        this.outputWriters = outputWriters;
    }

    public List<OutputWriter> getOutputWriters() {
        return outputWriters;
    }
    
    public void addOutputWriter(OutputWriter filter) {
        if (this.outputWriters == null) {
            this.outputWriters = new ArrayList<OutputWriter>();
        }
        this.outputWriters.add(filter);
    }

    @JsonIgnore
    public void setServer(Server server) {
        this.server = server;
    }

    @JsonIgnore
    public Server getServer() {
        return server;
    }

    @Override
    public String toString() {
        return "Query [obj=" + obj + ", attr=" + attr + "]";
    }
}
