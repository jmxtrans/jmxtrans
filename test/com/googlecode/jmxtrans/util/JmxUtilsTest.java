package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JmxUtilsTest {

	@Test
	public void mergeAlreadyExistingServerDoesNotModifyList() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(createServerWithOneQuery("example.net", "123", "toto"));

		JmxUtils.mergeServerLists(existingServers, newServers);

		assertThat(existingServers.size(), is(1));
		Server mergedServer = existingServers.get(0);
		assertThat(mergedServer.getQueries().size(), is(1));
	}

	@Test
	public void sameServerWithTwoDifferentQueriesMergesQueries() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(createServerWithOneQuery("example.net", "123", "tutu"));

		JmxUtils.mergeServerLists(existingServers, newServers);

		assertThat(existingServers.size(), is(1));
		Server mergedServer = existingServers.get(0);
		assertThat(mergedServer.getQueries().size(), is(2));
	}


	private Server createServerWithOneQuery(String host, String port, String queryObject) throws ValidationException {
		Server server = new Server(host, port);
		server.addQuery(new Query(queryObject));
		return server;
	}

}
