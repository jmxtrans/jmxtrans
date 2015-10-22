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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.naming.typename.PrependingTypeNameValuesStringBuilder;
import com.googlecode.jmxtrans.model.naming.typename.TypeNameValuesStringBuilder;
import com.googlecode.jmxtrans.model.naming.typename.UseAllTypeNameValuesStringBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.googlecode.jmxtrans.model.PropertyResolver.resolveList;
import static java.util.Arrays.asList;

/**
 * Represents a JMX Query to ask for obj, attr and one or more keys.
 *
 * @author jon
 */
@JsonSerialize(include = NON_NULL)
@JsonPropertyOrder(value = {"obj", "attr", "typeNames", "resultAlias", "keys", "allowDottedKeys", "useAllTypeNames", "outputWriters"})
@ThreadSafe
@Immutable // Note that outputWriters is neither thread safe nor immutable (yet)
public class Query {

	/** The JMX object representation: java.lang:type=Memory */
	@Getter private final String obj;
	@Nonnull @Getter private final ImmutableList<String> keys;

	@Nonnull @Getter private final ImmutableList<String> attr;

	/**
	 * The list of type names used in a JMX bean string when querying with a
	 * wildcard which is used to expose the actual type name value to the key
	 * string. e.g. for this JMX name
	 * <p/>
	 * typeName=name=PS Eden Space,type=MemoryPool
	 * <p/>
	 * If you add a typeName("name"), then it'll retrieve 'PS Eden Space' from
	 * the string
	 */
	@Getter private final ImmutableSet<String> typeNames;

	/**
	 * The alias allows you to specify what you would like the results of the
	 * query to go into.
	 */
	@Getter private final String resultAlias;

	/**
	 * The useObjDomainAsKey property allows you to specify the use of the Domain portion of the Object Name
	 * as part of the output key instead of using the ClassName of the MBean which is the default behavior.
	 */
	@Getter private final boolean useObjDomainAsKey;
	@Getter private final boolean allowDottedKeys;
	@Getter private final boolean useAllTypeNames;
	@Nonnull @Getter private final ImmutableList<OutputWriter> outputWriters;
	private final TypeNameValuesStringBuilder typeNameValuesStringBuilder;

	@JsonCreator
	public Query(
			@JsonProperty("obj") String obj,
			@JsonProperty("keys") List<String> keys,
			@JsonProperty("attr") List<String> attr,
			@JsonProperty("typeNames") Set<String> typeNames,
			@JsonProperty("resultAlias") String resultAlias,
			@JsonProperty("useObjDomainAsKey") boolean useObjDomainAsKey,
			@JsonProperty("allowDottedKeys") boolean allowDottedKeys,
			@JsonProperty("useAllTypeNames") boolean useAllTypeNames,
			@JsonProperty("outputWriters") List<OutputWriter> outputWriters
	) {
		this.obj = obj;
		this.attr = resolveList(firstNonNull(attr, Collections.<String>emptyList()));
		this.resultAlias = resultAlias;
		this.useObjDomainAsKey = firstNonNull(useObjDomainAsKey, false);
		this.keys = resolveList(firstNonNull(keys, Collections.<String>emptyList()));
		this.allowDottedKeys = allowDottedKeys;
		this.useAllTypeNames = useAllTypeNames;
		this.outputWriters = ImmutableList.copyOf(firstNonNull(outputWriters, Collections.<OutputWriter>emptyList()));
		this.typeNames = ImmutableSet.copyOf(firstNonNull(typeNames, Collections.<String>emptySet()));

		this.typeNameValuesStringBuilder = makeTypeNameValuesStringBuilder();
	}

	public String makeTypeNameValueString(List<String> typeNames, String typeNameStr) {
		return this.typeNameValuesStringBuilder.build(typeNames, typeNameStr);
	}

	@Override
	public String toString() {
		return "Query [obj=" + obj + ", useObjDomainAsKey:" + useObjDomainAsKey + 
				", resultAlias=" + resultAlias + ", attr=" + attr + "]";
	}

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

		return new EqualsBuilder()
				.append(this.getObj(), other.getObj())
				.append(this.getKeys(), other.getKeys())
				.append(this.getAttr(), other.getAttr())
				.append(this.getResultAlias(), other.getResultAlias())
				.append(sizeOf(this.getOutputWriters()), sizeOf(other.getOutputWriters()))
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(41, 97)
				.append(this.getObj())
				.append(this.getKeys())
				.append(this.getAttr())
				.append(this.getResultAlias())
				.append(sizeOf(this.getOutputWriters()))
				.toHashCode();
	}

	private static int sizeOf(List<OutputWriter> writers) {
		if (writers == null) {
			return 0;
		}
		return writers.size();
	}

	private TypeNameValuesStringBuilder makeTypeNameValuesStringBuilder() {
		String separator = isAllowDottedKeys() ? "." : TypeNameValuesStringBuilder.DEFAULT_SEPARATOR;
		Set<String> typeNames = getTypeNames();
		if (isUseAllTypeNames()) {
			return new UseAllTypeNameValuesStringBuilder(separator);
		} else if (typeNames != null && typeNames.size() > 0) {
			return new PrependingTypeNameValuesStringBuilder(separator, new ArrayList<String>(typeNames));
		} else {
			return new TypeNameValuesStringBuilder(separator);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	@NotThreadSafe
	@Accessors(chain = true)
	public static final class Builder {
		@Setter private String obj;
		private final List<String> attr = newArrayList();
		@Setter private String resultAlias;
		private final List<String> keys = newArrayList();
		@Setter private boolean useObjDomainAsKey;
		@Setter private boolean allowDottedKeys;
		@Setter private boolean useAllTypeNames;
		private final List<OutputWriter> outputWriters = newArrayList();
		private final Set<String> typeNames = newHashSet();

		private Builder() {}

		public Builder addAttr(String... attr) {
			this.attr.addAll(asList(attr));
			return this;
		}

		public Builder addKey(String keys) {
			return addKeys(keys);
		}

		public Builder addKeys(String... keys) {
			this.keys.addAll(asList(keys));
			return this;
		}

		public Builder addOutputWriter(OutputWriter outputWriter) {
			return addOutputWriters(outputWriter);
		}

		public Builder addOutputWriters(OutputWriter... outputWriters) {
			this.outputWriters.addAll(asList(outputWriters));
			return this;
		}

		public Builder setTypeNames(Set<String> typeNames) {
			this.typeNames.addAll(typeNames);
			return this;
		}

		public Query build() {
			return new Query(
					this.obj,
					this.keys,
					this.attr,
					this.typeNames,
					this.resultAlias,
					this.useObjDomainAsKey,
					this.allowDottedKeys,
					this.useAllTypeNames,
					this.outputWriters
			);
		}

	}
}
