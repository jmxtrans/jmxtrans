package com.googlecode.jmxtrans.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;

/**
 * Represents the result of a query.
 * 
 * @author jon
 */
@JsonSerialize(include = NON_NULL)
@ThreadSafe
@Immutable
public class Result {
	private final String attributeName;
	private final String className;
	private final String objDomain;
	private final String typeName;
	private final ImmutableMap<String, Object> values;
	private final long epoch;
	private final String keyAlias;

	public Result(long epoch, String attributeName, String className, String objDomain, String keyAlias, String typeName, Map<String, Object> values) {
		this.className = className;
		this.objDomain = objDomain;
		this.typeName = typeName;
		this.values = ImmutableMap.copyOf(values);
		this.epoch = epoch;
		this.attributeName = attributeName;
		this.keyAlias = keyAlias;
	}

	public String getClassName() {
		return className;
	}
	
	public String getObjDomain() {
		return objDomain;
	}

	/**
	 * Specified as part of the query.
	 */
	public String getKeyAlias() {
		return keyAlias;
	}

	public String getTypeName() {
		return typeName;
	}

	@Nonnull
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
		return "Result [attributeName=" + attributeName + ", className=" + className + ", objDomain=" + objDomain + ", typeName=" + typeName + ", values=" + values + ", epoch="
				+ epoch + "]";
	}
}
