package com.googlecode.jmxtrans.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
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
@JsonPropertyOrder(value = {"obj", "attr", "typeNames", "resultAlias", "keys", "allowDottedKeys", "outputWriters"})
@ThreadSafe
@Immutable // Note that outputWriters is neither thread safe nor immutable (yet)
public class Query {

	private final String obj;
	private final ImmutableList<String> keys;
	private final ImmutableList<String> attr;
	private final ImmutableSet<String> typeNames;
	private final String resultAlias;
	private final boolean useObjDomainAsKey;
	private final boolean allowDottedKeys;
	private final ImmutableList<OutputWriter> outputWriters;

	@JsonCreator
	public Query(
			@JsonProperty("obj") String obj,
			@JsonProperty("keys") List<String> keys,
			@JsonProperty("attr") List<String> attr,
			@JsonProperty("typeNames") Set<String> typeNames,
			@JsonProperty("resultAlias") String resultAlias,
			@JsonProperty("useObjDomainAsKey") boolean useObjDomainAsKey,
			@JsonProperty("allowDottedKeys") boolean allowDottedKeys,
			@JsonProperty("outputWriters") List<OutputWriter> outputWriters
	) {
		this.obj = obj;
		this.attr = resolveList(firstNonNull(attr, Collections.<String>emptyList()));
		this.resultAlias = resultAlias;
		this.useObjDomainAsKey = firstNonNull(useObjDomainAsKey, false);
		this.keys = resolveList(firstNonNull(keys, Collections.<String>emptyList()));
		this.allowDottedKeys = allowDottedKeys;
		this.outputWriters = ImmutableList.copyOf(firstNonNull(outputWriters, Collections.<OutputWriter>emptyList()));
		this.typeNames = ImmutableSet.copyOf(firstNonNull(typeNames, Collections.<String>emptySet()));
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
	public String getResultAlias() {
		return resultAlias;
	}

	/**
	 * The useObjDomainAsKey property allows you to specify the use of the Domain portion of the Object Name
	 * as part of the output key instead of using the ClassName of the MBean which is the default behavior.
	 */
	public boolean isUseObjDomainAsKey() {
		return useObjDomainAsKey;
	}

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
	public ImmutableSet<String> getTypeNames() {
		return typeNames;
	}

	@Nonnull
	public ImmutableList<String> getAttr() {
		return attr;
	}

	@Nonnull
	public ImmutableList<String> getKeys() {
		return keys;
	}

	public boolean isAllowDottedKeys() {
		return allowDottedKeys;
	}

	@Nonnull
	public ImmutableList<OutputWriter> getOutputWriters() {
		return outputWriters;
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

	public static Builder builder() {
		return new Builder();
	}

	@NotThreadSafe
	public static final class Builder {

		private String obj;
		private final List<String> attr = newArrayList();
		private String resultAlias;
		private final List<String> keys = newArrayList();
		private boolean useObjDomainAsKey;
		private boolean allowDottedKeys;
		private final List<OutputWriter> outputWriters = newArrayList();
		private final Set<String> typeNames = newHashSet();

		private Builder() {}

		public Builder setObj(String obj) {
			this.obj = obj;
			return this;
		}

		public Builder addAttr(String... attr) {
			this.attr.addAll(asList(attr));
			return this;
		}

		public Builder setResultAlias(String resultAlias) {
			this.resultAlias = resultAlias;
			return this;
		}
		
		public Builder setUseObjDomainAsKey(boolean useObjDomainAsKey) {
			this.useObjDomainAsKey = useObjDomainAsKey;
			return this;
		}

		public Builder addKey(String keys) {
			return addKeys(keys);
		}

		public Builder addKeys(String... keys) {
			this.keys.addAll(asList(keys));
			return this;
		}

		public Builder setAllowDottedKeys(boolean allowDottedKeys) {
			this.allowDottedKeys = allowDottedKeys;
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
					this.outputWriters
			);
		}

	}
}
