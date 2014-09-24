package com.googlecode.jmxtrans.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.util.JmxUtils.getConcatedTypeNameValues;
import static com.googlecode.jmxtrans.util.JmxUtils.getTypeNameValueMap;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Zack Radick Date: 1/20/12
 * @author Arthur Naseef Date: 02/23/2014
 */
public class JmxUtilsTests {

	@Test
	public void testGetTypeNameValueMap() {
		assertThat(getTypeNameValueMap(null)).isEmpty();
		assertThat(getTypeNameValueMap("")).isEmpty();
		assertThat(getTypeNameValueMap("x-key1-x")).isEqualTo(makeMap("x-key1-x", ""));
		assertThat(getTypeNameValueMap("x-key1-x,x-key2-x")).isEqualTo(makeMap("x-key1-x", "", "x-key2-x", ""));
		assertThat(getTypeNameValueMap("x-key1-x=x-value1-x")).isEqualTo(makeMap("x-key1-x", "x-value1-x"));

		assertThat(getTypeNameValueMap("x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "y-value2-y"));
		assertThat(getTypeNameValueMap("x-key1-x=x-value1-x,y-key2-y=y-value2-y,z-key3-z=z-value3-z"))
				.isEqualTo(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "y-value2-y", "z-key3-z", "z-value3-z"));
		assertThat(getTypeNameValueMap("x-key1-x=x-value1-x,y-key2-y,yy-key2.5-yy=a=1,z-key3-z=z-value3-z"))
				.isEqualTo(makeMap("x-key1-x", "x-value1-x", "y-key2-y", "", "yy-key2.5-yy", "a=1", "z-key3-z", "z-value3-z"));
	}


	@Test
	public void testGetConcatenatedTypeNameValues() {
		assertThat(getConcatedTypeNameValues(null, "a=1")).isNull();
		assertThat(getConcatedTypeNameValues(Collections.<String>emptyList(), "a=1")).isNull();
		assertThat(getConcatedTypeNameValues(asList("x-key1-x"), "")).isEmpty();
		assertThat(getConcatedTypeNameValues(asList("x-key1-x", "y-key2-y"), "")).isEmpty();
		assertThat(getConcatedTypeNameValues(asList("x-key1-x"), "x-key1-x=x-value1-x")).isEqualTo("x-value1-x");
		assertThat(getConcatedTypeNameValues(asList("x-key1-x"), "y-key2-y=y-value2-y")).isEmpty();
		assertThat(getConcatedTypeNameValues(asList("x-key1-x", "y-key2-y"), "x-key1-x=x-value1-x,y-key2-y=y-value2-y"))
				.isEqualTo("x-value1-x_y-value2-y");
	}


	@Test
	public void mergeAlreadyExistingServerDoesNotModifyList() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(createServerWithOneQuery("example.net", "123", "toto"));

		JmxUtils.mergeServerLists(existingServers, newServers);

		assertThat(existingServers).hasSize(1);

		Server mergedServer = existingServers.get(0);
		assertThat(mergedServer.getQueries()).hasSize(1);
	}

	@Test
	public void sameServerWithTwoDifferentQueriesMergesQueries() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(createServerWithOneQuery("example.net", "123", "tutu"));

		JmxUtils.mergeServerLists(existingServers, newServers);

		assertThat(existingServers).hasSize(1);
		Server mergedServer = existingServers.get(0);
		assertThat(mergedServer.getQueries()).hasSize(2);
	}

	private Server createServerWithOneQuery(String host, String port, String queryObject) throws ValidationException {
		Server server = new Server(host, port);
		server.addQuery(new Query(queryObject));
		return server;
	}

	/**
	 * Convenience method for creating a Map for comparison.
	 */
	protected Map<String, String> makeMap(String... keysAndValues) {
		Map<String, String> result;
		int cur;

		result = newHashMap();

		cur = 0;
		while (cur < keysAndValues.length) {
			if (cur < keysAndValues.length - 1) {
				result.put(keysAndValues[cur], keysAndValues[cur + 1]);
				cur += 2;
			} else {
				result.put(keysAndValues[cur], "");
				cur++;
			}
		}

		return result;
	}
}
