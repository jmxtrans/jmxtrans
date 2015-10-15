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
package com.googlecode.jmxtrans;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ServerFixtures;
import com.googlecode.jmxtrans.model.ValidationException;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationParserTest {

	@Test(expected = LifecycleException.class)
	public void failParsingOnErrorIfRequested() throws URISyntaxException, LifecycleException {
		File validInput = new File(ConfigurationParserTest.class.getResource("/example.json").toURI());
		File invalidInput = new File("/non/existing/file");

		boolean continueOnJsonError = false;

		new ConfigurationParser().parseServers(of(validInput, invalidInput), continueOnJsonError);
	}

	@Test
	public void continueParsingOnErrorIfRequested() throws URISyntaxException, LifecycleException {
		File validInput = new File(ConfigurationParserTest.class.getResource("/example.json").toURI());
		File invalidInput = new File("/non/existing/file");

		boolean continueOnJsonError = true;

		ImmutableList servers = new ConfigurationParser().parseServers(of(validInput, invalidInput), continueOnJsonError);

		assertThat(servers).hasSize(1);
	}

	@Test
	public void mergeAlreadyExistingServerDoesNotModifyList() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> merged = new ConfigurationParser().mergeServerLists(existingServers, newServers);

		assertThat(merged).hasSize(1);

		Server mergedServer = merged.get(0);
		assertThat(mergedServer.getQueries()).hasSize(1);
	}

	@Test
	public void sameServerWithTwoDifferentQueriesMergesQueries() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "tutu"));

		List<Server> merged = new ConfigurationParser().mergeServerLists(existingServers, newServers);

		assertThat(merged).hasSize(1);
		Server mergedServer = merged.get(0);
		assertThat(mergedServer.getQueries()).hasSize(2);
	}


	@Test
	public void testMerge() throws Exception {
		Query q1 = Query.builder()
				.setObj("obj")
				.addAttr("foo")
				.addAttr("bar")
				.addKey("key1")
				.addKey("key2")
				.setResultAlias("alias")
				.build();

		// same as q1
		Query q2 = Query.builder()
				.setObj("obj")
				.addAttr("foo")
				.addAttr("bar")
				.addKey("key1")
				.addKey("key2")
				.setResultAlias("alias")
				.build();

		// different than q1 and q2
		Query q3 = Query.builder()
				.setObj("obj3")
				.addAttr("foo")
				.addAttr("bar")
				.addKey("key1")
				.addKey("key2")
				.setResultAlias("alias")
				.build();

		Server s1 = Server.builder()
				.setAlias("alias")
				.setHost("host")
				.setPort("8004")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.addQuery(q1)
				.addQuery(q2)
				.build();

		// same as s1
		Server s2 = Server.builder()
				.setAlias("alias")
				.setHost("host")
				.setPort("8004")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.addQuery(q1)
				.addQuery(q2)
				.build();

		Server s3 = Server.builder()
				.setAlias("alias")
				.setHost("host3")
				.setPort("8004")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.addQuery(q1)
				.addQuery(q2)
				.addQuery(q3)
				.build();

		List<Server> existing = new ArrayList<Server>();
		existing.add(s1);

		List<Server> adding = new ArrayList<Server>();

		adding.add(s2);
		existing = new ConfigurationParser().mergeServerLists(existing, adding);

		// should only have one server with 1 query since we just added the same
		// server and same query.
		assertThat(existing).hasSize(1);
		assertThat(existing.get(0).getQueries()).hasSize(1);

		adding.add(s3);
		existing = new ConfigurationParser().mergeServerLists(existing, adding);

		assertThat(existing).hasSize(2);
		assertThat(existing.get(0).getQueries()).hasSize(1);
		assertThat(existing.get(1).getQueries()).hasSize(2);
	}

}
