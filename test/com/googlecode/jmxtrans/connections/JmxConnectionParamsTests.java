package com.googlecode.jmxtrans.connections;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxConnectionParamsTests {

	private static final String JMX_URL = "service:jmx:rmi:///jndi/rmi://:9999/jmxrmi";

	private static final String JMX_URL_2 = "service:jmx:rmi:///jndi/rmi://:8888/jmxrmi";

	@Test
	public void twoDifferentConnectionParamsHaveTheSameHashCode() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value"));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value"));
		assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
	}

	@Test
	public void connectionParamsWithDifferentEnvironmentsHaveDifferentHashCodes() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value1"));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value2"));
		assertThat(p1.hashCode()).isNotEqualTo(p2.hashCode());
	}

	@Test
	public void connectionParamsSameUrlAndNoEnvironmentHaveSameHashCodes() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.<String, Object>of());
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.<String, Object>of());
		assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
	}

	@Test
	public void connectionParamsDifferentUrlAndNoEnvironmentHaveDifferentHashCodes() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.<String, Object>of());
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL_2),
				ImmutableMap.<String, Object>of());
		assertThat(p1.hashCode()).isNotEqualTo(p2.hashCode());
	}

}
