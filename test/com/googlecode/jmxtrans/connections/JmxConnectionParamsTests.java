package com.googlecode.jmxtrans.connections;

import com.google.common.collect.ImmutableList;
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

	@Test
	public void twoDifferentConnectionParamsAreEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value"));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value"));
		assertThat(p1).isEqualTo(p2);
	}

	@Test
	public void connectionParamsWithDifferentEnvironmentsAreNotEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value1"));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("key", "value2"));
		assertThat(p1).isNotEqualTo(p2);
	}

	@Test
	public void connectionParamsSameUrlAndNoEnvironmentAreEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.<String, Object>of());
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.<String, Object>of());
		assertThat(p1).isEqualTo(p2);
	}

	@Test
	public void connectionParamsDifferentUrlAndNoEnvironmentAreNotEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.<String, Object>of());
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL_2),
				ImmutableMap.<String, Object>of());
		assertThat(p1).isNotEqualTo(p2);
	}

	@Test
	public void connectionParamsWithArrayAsEnvironmentValueAreEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", new String[]{ "value" }));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", new String[]{ "value" }));
		assertThat(p1).isEqualTo(p2);
	}

	@Test
	public void connectionParamsWithDifferentArrayAsEnvironmentValueAreNotEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", new String[]{ "value1" }));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", new String[]{ "value2" }));
		assertThat(p1).isNotEqualTo(p2);
	}

	@Test
	public void connectionParamsWithListsAsEnvironmentValueAreEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", ImmutableList.of("value")));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", ImmutableList.of("value")));
		assertThat(p1).isEqualTo(p2);
	}

	@Test
	public void connectionParamsWithDifferentListsAsEnvironmentValueAreNotEquals() throws MalformedURLException {
		JMXConnectionParams p1 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", ImmutableList.of("value1")));
		JMXConnectionParams p2 = new JMXConnectionParams(
				new JMXServiceURL(JMX_URL),
				ImmutableMap.of("test", ImmutableList.of("value2")));
		assertThat(p1).isNotEqualTo(p2);
	}

}
