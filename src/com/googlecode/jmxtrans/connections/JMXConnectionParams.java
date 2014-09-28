package com.googlecode.jmxtrans.connections;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.management.remote.JMXServiceURL;

/*
 * FIXME
 * This class has been extracted as a simple holder to break the dependency
 * between the JMXConnectionFactory and the Server. There is probably a cleaner
 * way to do this, but this will come later.
 */
public class JMXConnectionParams {
	private final JMXServiceURL url;
	private final ImmutableMap<String, ?> environment;

	public JMXConnectionParams(JMXServiceURL url, ImmutableMap<String, ?> environment) {
		this.url = url;
		this.environment = environment;
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
				.append(this.environment, that.environment)
				.append(this.url, that.url)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(135, 211)
				.append(this.environment)
				.append(this.url)
				.toHashCode();
	}
}
