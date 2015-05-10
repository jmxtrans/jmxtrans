package com.googlecode.jmxtrans.connections;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.management.remote.JMXServiceURL;
import java.lang.reflect.Array;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.reflect.Array.getLength;

/*
 * TODO This class has been extracted as a simple holder to break the
 * dependency between the JMXConnectionFactory and the Server. There is
 * probably a cleaner way to do this, but this will come later.
 */
public class JMXConnectionParams {
	private final JMXServiceURL url;
	private final ImmutableMap<String, ?> environment;

	public JMXConnectionParams(JMXServiceURL url, Map<String, ?> environment) {
		this.url = url;
		this.environment = ImmutableMap.copyOf(environment);
	}

	public JMXServiceURL getUrl() {
		return url;
	}

	public ImmutableMap<String, ?> getEnvironment() {
		return environment;
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

		if (!(o instanceof JMXConnectionParams)) {
			return false;
		}

		JMXConnectionParams that = (JMXConnectionParams) o;

		return new EqualsBuilder()
				.append(convertArraysToLists(this.environment), convertArraysToLists(that.environment))
				.append(this.url, that.url)
				.isEquals();
	}

	/**
	 * Convert values in a map to ensure all arrays are transformed to lists.
	 *
	 * This is an ugly workaround for https://github.com/jmxtrans/jmxtrans/issues/190. We need to ensure that
	 * environments containing arrays of same values are treated as equals for the purpose of hashCode() and equals().
	 */
	private ImmutableMap<String, ?> convertArraysToLists(ImmutableMap<String, ?> map) {
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			if (entry.getValue().getClass().isArray()) {
				builder.put(entry.getKey(), asList(entry.getValue()));
			} else {
				builder.put(entry.getKey(), entry.getValue());
			}
		}
		return builder.build();
	}

	private ImmutableList<?> asList(Object array) {
		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		for (int i = 0; i < getLength(array); i++) {
			builder.add(Array.get(array, i));
		}
		return builder.build();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(135, 211)
				.append(convertArraysToLists(this.environment))
				.append(this.url)
				.toHashCode();
	}

	@Override
	public String toString() {
		return toStringHelper(getClass())
				.add("url", url)
				.add("environment", environment)
				.toString();
	}

}
