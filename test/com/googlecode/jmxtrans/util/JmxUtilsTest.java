package com.googlecode.jmxtrans.util;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class JmxUtilsTest {

    @Test
    public void mergeAlreadyExistingServerDoesNotModifyList() throws ValidationException {
        List<Server> existingServers = new ArrayList<Server>();
        existingServers.add(createServerWithOneQuery("example.net", "123", "toto"));

        List<Server> newServers = new ArrayList<Server>();
        newServers.add(createServerWithOneQuery("example.net", "123", "toto"));

        JmxUtils.mergeServerLists(existingServers, newServers);

        MatcherAssert.assertThat(existingServers.size(), Matchers.is(1));
        Server mergedServer = existingServers.get(0);
        MatcherAssert.assertThat(mergedServer.getQueries().size(), Matchers.is(1));
    }

    @Test
    public void sameServerWithTwoDifferentQueriesMergesQueries() throws ValidationException {
        List<Server> existingServers = new ArrayList<Server>();
        existingServers.add(createServerWithOneQuery("example.net", "123", "toto"));

        List<Server> newServers = new ArrayList<Server>();
        newServers.add(createServerWithOneQuery("example.net", "123", "tutu"));

        JmxUtils.mergeServerLists(existingServers, newServers);

        MatcherAssert.assertThat(existingServers.size(), Matchers.is(1));
        Server mergedServer = existingServers.get(0);
        MatcherAssert.assertThat(mergedServer.getQueries().size(), Matchers.is(2));
    }


    private Server createServerWithOneQuery(String host, String port, String queryObject) throws ValidationException {
        Server server = new Server(host, port);
        server.addQuery(new Query(queryObject));
        return server;
    }

}
