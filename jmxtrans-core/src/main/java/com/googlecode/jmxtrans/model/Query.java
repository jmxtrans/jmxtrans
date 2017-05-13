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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.naming.typename.PrependingTypeNameValuesStringBuilder;
import com.googlecode.jmxtrans.model.naming.typename.TypeNameValuesStringBuilder;
import com.googlecode.jmxtrans.model.naming.typename.UseAllTypeNameValuesStringBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_NULL;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;

/**
 * Represents a JMX Query to ask for obj, attr and one or more keys.
 *
 * @author jon
 */
@JsonSerialize(include = NON_NULL)
@JsonPropertyOrder(value = {"obj", "attr", "typeNames", "resultAlias", "keys", "allowDottedKeys", "useAllTypeNames", "outputWriters"})
@ThreadSafe
@EqualsAndHashCode(exclude = {"outputWriters", "outputWriterInstances"})
@ToString(exclude = {"outputWriters", "typeNameValuesStringBuilder"})
public class Query {

	private static final Logger logger = LoggerFactory.getLogger(Query.class);

	/** The JMX object representation: java.lang:type=Memory */
	@Nonnull @Getter private final ObjectName objectName;
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
	 * the string.
	 * <p>
	 * The order of the elements of this set matches the order provided by the
	 * user.
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
	@Nonnull @Getter private final ImmutableList<OutputWriterFactory> outputWriters;
	@Nonnull @Getter private final Iterable<OutputWriter> outputWriterInstances;
	private final TypeNameValuesStringBuilder typeNameValuesStringBuilder;

	@JsonCreator
	public Query(
			@JsonProperty("obj") String obj,
			@JsonProperty("keys") List<String> keys,
			@JsonProperty("attr") List<String> attr,
			@JsonProperty("typeNames") List<String> typeNames,
			@JsonProperty("resultAlias") String resultAlias,
			@JsonProperty("useObjDomainAsKey") boolean useObjDomainAsKey,
			@JsonProperty("allowDottedKeys") boolean allowDottedKeys,
			@JsonProperty("useAllTypeNames") boolean useAllTypeNames,
			@JsonProperty("outputWriters") List<OutputWriterFactory> outputWriters
	) {
		// For typeName, note the using copyOf does not change the order of
		// the elements.
		this(obj, keys, attr, ImmutableSet.copyOf(firstNonNull(typeNames, Collections.<String>emptySet())), resultAlias, useObjDomainAsKey, allowDottedKeys, useAllTypeNames,
				outputWriters, ImmutableList.<OutputWriter>of());
	}

	public Query(
			String obj,
			List<String> keys,
			List<String> attr,
			Set<String> typeNames,
			String resultAlias,
			boolean useObjDomainAsKey,
			boolean allowDottedKeys,
			boolean useAllTypeNames,
			List<OutputWriterFactory> outputWriters
	) {
		this(obj, keys, attr, typeNames, resultAlias, useObjDomainAsKey, allowDottedKeys, useAllTypeNames,
				outputWriters, ImmutableList.<OutputWriter>of());
	}

	public Query(
			String obj,
			List<String> keys,
			List<String> attr,
			Set<String> typeNames,
			String resultAlias,
			boolean useObjDomainAsKey,
			boolean allowDottedKeys,
			boolean useAllTypeNames,
			ImmutableList<OutputWriter> outputWriters
	) {
		this(obj, keys, attr, typeNames, resultAlias, useObjDomainAsKey, allowDottedKeys, useAllTypeNames,
				ImmutableList.<OutputWriterFactory>of(), outputWriters);
	}

	private Query(
			String obj,
			List<String> keys,
			List<String> attr,
			Set<String> typeNames,
			String resultAlias,
			boolean useObjDomainAsKey,
			boolean allowDottedKeys,
			boolean useAllTypeNames,
			List<OutputWriterFactory> outputWriterFactories,
			List<OutputWriter> outputWriters
	) {
		try {
			this.objectName = new ObjectName(obj);
		} catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException("Invalid object name: " + obj, e);
		}
		this.attr = copyOf(firstNonNull(attr, Collections.<String>emptyList()));
		this.resultAlias = resultAlias;
		this.useObjDomainAsKey = firstNonNull(useObjDomainAsKey, false);
		this.keys = copyOf(firstNonNull(keys, Collections.<String>emptyList()));
		this.allowDottedKeys = allowDottedKeys;
		this.useAllTypeNames = useAllTypeNames;
		this.outputWriters = copyOf(firstNonNull(outputWriterFactories, ImmutableList.<OutputWriterFactory>of()));
		// We need to preserve the order of typeNames. So note that copyOf
		// does not mess with the order. 
		this.typeNames = ImmutableSet.copyOf(firstNonNull(typeNames, Collections.<String>emptySet()));

		this.typeNameValuesStringBuilder = makeTypeNameValuesStringBuilder();

		this.outputWriterInstances = copyOf(firstNonNull(outputWriters, ImmutableList.<OutputWriter>of()));
	}

	public String makeTypeNameValueString(List<String> typeNames, String typeNameStr) {
		return this.typeNameValuesStringBuilder.build(typeNames, typeNameStr);
	}

	public Iterable<ObjectName> queryNames(MBeanServerConnection mbeanServer) throws IOException {
		return mbeanServer.queryNames(objectName, null);
	}

	public Iterable<Result> fetchResults(MBeanServerConnection mbeanServer, ObjectName queryName) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		ObjectInstance oi = mbeanServer.getObjectInstance(queryName);

		List<String> attributes;
		if (attr.isEmpty()) {
			attributes = new ArrayList<>();
			MBeanInfo info = mbeanServer.getMBeanInfo(queryName);
			for (MBeanAttributeInfo attrInfo : info.getAttributes()) {
				attributes.add(attrInfo.getName());
			}
		} else {
			attributes = attr;
		}

		try {
			if (!attributes.isEmpty()) {
				logger.debug("Executing queryName [{}] from query [{}]", queryName.getCanonicalName(), this);

				AttributeList al = mbeanServer.getAttributes(queryName, attributes.toArray(new String[attributes.size()]));

				return new JmxResultProcessor(this, oi, al.asList(), oi.getClassName(), queryName.getDomain()).getResults();
			}
		} catch (UnmarshalException ue) {
			if ((ue.getCause() != null) && (ue.getCause() instanceof ClassNotFoundException)) {
				logger.debug("Bad unmarshall, continuing. This is probably ok and due to something like this: "
						+ "http://ehcache.org/xref/net/sf/ehcache/distribution/RMICacheManagerPeerListener.html#52", ue.getMessage());
			} else {
				throw ue;
			}
		}
		return ImmutableList.of();
	}

	private TypeNameValuesStringBuilder makeTypeNameValuesStringBuilder() {
		String separator = isAllowDottedKeys() ? "." : TypeNameValuesStringBuilder.DEFAULT_SEPARATOR;
		Set<String> typeNames = getTypeNames();
		if (isUseAllTypeNames()) {
			return new UseAllTypeNameValuesStringBuilder(separator);
		} else if (typeNames != null && !typeNames.isEmpty()) {
			return new PrependingTypeNameValuesStringBuilder(separator, new ArrayList<>(typeNames));
		} else {
			return new TypeNameValuesStringBuilder(separator);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(Query query) {
		return new Builder(query);
	}

	public void runOutputWritersForQuery(Server server, Iterable<Result> results) throws Exception {
		for (OutputWriter writer : getOutputWriterInstances()) {
			writer.doWrite(server, this, results);
		}
		logger.debug("Finished running outputWriters for query: {}", this);
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
		private final List<OutputWriterFactory> outputWriterFactories = newArrayList();
		private final List<OutputWriter> outputWriters = newArrayList();
		// We need to pick an order preserving Set implementation here to
		// avoid unpredictable ordering of typeNames.
		private final Set<String> typeNames = newLinkedHashSet();

		private Builder() {}

		/** This builder does NOT copy output writers from the given query. */
		private Builder(Query query) {
			this.obj = query.objectName.toString();
			this.attr.addAll(query.attr);
			this.resultAlias = query.resultAlias;
			this.keys.addAll(query.keys);
			this.useObjDomainAsKey = query.useObjDomainAsKey;
			this.allowDottedKeys = query.allowDottedKeys;
			this.useAllTypeNames = query.useAllTypeNames;
			this.typeNames.addAll(query.typeNames);
		}

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

		public Builder addOutputWriterFactory(OutputWriterFactory outputWriterFactory) {
			return addOutputWriterFactories(outputWriterFactory);
		}

		public Builder addOutputWriterFactories(OutputWriterFactory... outputWriterFactories) {
			this.outputWriterFactories.addAll(asList(outputWriterFactories));
			return this;
		}

		public Builder addOutputWriters(Collection<OutputWriter> outputWriters) {
			this.outputWriters.addAll(outputWriters);
			return this;
		}

		public Builder setTypeNames(Collection<String> typeNames) {
			this.typeNames.addAll(typeNames);
			return this;
		}

		public Query build() {
			if (!outputWriterFactories.isEmpty()) {
				return new Query(
						this.obj,
						this.keys,
						this.attr,
						this.typeNames,
						this.resultAlias,
						this.useObjDomainAsKey,
						this.allowDottedKeys,
						this.useAllTypeNames,
						this.outputWriterFactories
				);
			}
			return new Query(
					this.obj,
					this.keys,
					this.attr,
					this.typeNames,
					this.resultAlias,
					this.useObjDomainAsKey,
					this.allowDottedKeys,
					this.useAllTypeNames,
					copyOf(this.outputWriters)
			);
		}

	}
}
