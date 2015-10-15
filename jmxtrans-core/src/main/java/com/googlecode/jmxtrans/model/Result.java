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
