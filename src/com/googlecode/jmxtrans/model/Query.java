package com.googlecode.jmxtrans.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.util.PropertyResolver;

/**
 * Represents a JMX Query to ask for obj, attr and one or more keys.
 * 
 * Once the query has been executed, it'll have a list of results.
 * 
 * @author jon
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonPropertyOrder(value = { "obj", "attr", "typeNames", "resultAlias", "keys", "outputWriters" })
public class Query {

	private Server server;

	private String obj;
	private List<String> attr;
	private String resultAlias;
	private List<String> keys;
	private List<OutputWriter> outputWriters;
	private List<Result> results;
	private Set<String> typeNames;

	public Query() {
	}

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

	/**
	 * The JMX object representation: java.lang:type=Memory
	 */
	public void setObj(String obj) {
		this.obj = PropertyResolver.resolveProps(obj);
	}

	/**
	 * The JMX object representation: java.lang:type=Memory
	 */
	public String getObj() {
		return obj;
	}

	/**
	 * The alias allows you to specify what you would like the results of the
	 * query to go into.
	 */
	public void setResultAlias(String resultAlias) {
		this.resultAlias = resultAlias;
	}

	/**
	 * The alias allows you to specify what you would like the results of the
	 * query to go into.
	 */
	public String getResultAlias() {
		return resultAlias;
	}

	public void setTypeNames(Set<String> typeNames) {
		this.typeNames = typeNames;
	}

	/**
	 * The list of type names used in a JMX bean string when querying with a
	 * wildcard which is used to expose the actual type name value to the key
	 * string. e.g. for this JMX name
	 * 
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * 
	 * If you add a typeName("name"), then it'll retrieve 'PS Eden Space' from
	 * the string
	 */
	public Set<String> getTypeNames() {
		return typeNames;
	}

	public void setAttr(List<String> attr) {
		this.attr = attr;
		PropertyResolver.resolveList(this.attr);
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
		PropertyResolver.resolveList(this.keys);
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

	public void addOutputWriter(OutputWriter writer) {
		if (this.outputWriters == null) {
			this.outputWriters = new ArrayList<OutputWriter>();
		}
		this.outputWriters.add(writer);
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
		return "Query [obj=" + obj + ", resultAlias=" + resultAlias + ", attr=" + attr + "]";
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

		if (!(o instanceof Query)) {
			return false;
		}

		Query other = (Query) o;

		int sizeL = 0;
		int sizeR = 0;
		if (this.getOutputWriters() != null) {
			sizeL = this.getOutputWriters().size();
		}
		if (other.getOutputWriters() != null) {
			sizeR = other.getOutputWriters().size();
		}

		return new EqualsBuilder().append(this.getObj(), other.getObj()).append(this.getKeys(), other.getKeys())
				.append(this.getAttr(), other.getAttr()).append(this.getResultAlias(), other.getResultAlias()).append(sizeL, sizeR).isEquals();
	}

	/** */
	@Override
	public int hashCode() {
		int sizeL = 0;
		if (this.getOutputWriters() != null) {
			sizeL = this.getOutputWriters().size();
		}

		return new HashCodeBuilder(41, 97).append(this.getObj()).append(this.getKeys()).append(this.getAttr()).append(this.getResultAlias())
				.append(sizeL).toHashCode();
	}
}
