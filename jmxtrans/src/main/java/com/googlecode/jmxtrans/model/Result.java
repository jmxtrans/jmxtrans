package com.googlecode.jmxtrans.model;

import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Represents the result of a query.
 * 
 * @author jon
 */
@JsonSerialize(include = Inclusion.NON_NULL)
public class Result {
	private String attributeName;
	private String className;
	private String typeName;
	private Map<String, Object> values;
	private long epoch;
	private Query query;

	public Result() {
		epoch = System.currentTimeMillis();
	}

	public Result(String attributeName) {
		this();
		this.setAttributeName(attributeName);
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	@JsonIgnore
	public Query getQuery() {
		return query;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	/**
	 * Specified as part of the query.
	 */
	public String getClassNameAlias() {
		return query.getResultAlias();
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public void addValue(String key, Object value) {
		if (this.values == null) {
			values = new TreeMap<String, Object>();
		}
		if (query.getKeys() == null || query.getKeys().contains(key)) {
			values.put(key, value);
		}
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}

	public long getEpoch() {
		return this.epoch;
	}

	@Override
	public String toString() {
		return "Result [attributeName=" + attributeName + ", className=" + className + ", typeName=" + typeName + ", values=" + values + ", epoch="
				+ epoch + "]";
	}
}
