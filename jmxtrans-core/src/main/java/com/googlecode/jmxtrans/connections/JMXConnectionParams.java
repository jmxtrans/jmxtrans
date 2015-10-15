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
