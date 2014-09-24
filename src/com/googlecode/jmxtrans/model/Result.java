package com.googlecode.jmxtrans.model;

import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import java.util.Map;

/**
 * Represents the result of a query.
 * 
 * @author jon
 */
@JsonSerialize(include = Inclusion.NON_NULL)
public class Result {
	private final String attributeName;
	private final String className;
	private final String typeName;
	private final ImmutableMap<String, Object> values;
	private final long epoch;
	private final Query query;

	public Result(String attributeName, String className, String typeName, Map<String, Object> values, Query query) {
		this.className = className;
		this.typeName = typeName;
		this.values = ImmutableMap.copyOf(values);
		this.query = query;
		this.epoch = System.currentTimeMillis();
		this.attributeName = attributeName;
	}

	@JsonIgnore
	public Query getQuery() {
		return query;
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

	public String getTypeName() {
		return typeName;
	}

	public ImmutableMap<String, Object> getValues() {
		return values;
	}

	public String getAttributeName() {
		return attributeName;
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
