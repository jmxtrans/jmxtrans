/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
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
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.naming.typename.TypeNameValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
@EqualsAndHashCode
@ToString
public class Result {
	@Getter private final String attributeName;
	@Getter private final String className;
	@Getter private final String objDomain;
	@Getter private final String typeName;
	@Getter private final ImmutableList<String> valuePath;
	@Nonnull @Getter private final Object value;
	@Getter private final long epoch;
	/** Specified as part of the query. */
	@Getter private final String keyAlias;

	public Result(
			long epoch, String attributeName, String className, String objDomain, String keyAlias, String typeName,
			@Nonnull ImmutableList<String> valuePath, Object value) {
		this.className = className;
		this.objDomain = objDomain;
		this.typeName = typeName;
		this.valuePath = valuePath;
		this.value = value;
		this.epoch = epoch;
		this.attributeName = attributeName;
		this.keyAlias = keyAlias;
	}

	/**
	 * Get typeName split into a Map
     */
	public Map<String, String> getTypeNameMap() {
		return TypeNameValue.extractMap(this.typeName);
	}
}
