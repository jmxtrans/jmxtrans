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
package com.googlecode.jmxtrans;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Iterator;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.QueryFixtures.queryWithAllTypeNames;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.googlecode.jmxtrans.model.ServerFixtures.serverWithNoQuery;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ServerListBuilderTest {

	@Test
	public void serversAreMerged() {
		ImmutableList serverList = new ServerListBuilder()
				.add(ImmutableList.of(dummyServer(), dummyServer()))
				.build();
		assertThat(serverList).hasSize(1);
	}

	@Test
	public void outputWritersAreReusedOnServers() {
		Server server1 = Server.builder(dummyServer())
				.addOutputWriterFactory(new DummyOutputWriterFactory("output 1"))
				.addOutputWriterFactory(new DummyOutputWriterFactory("output 2"))
				.build();
		Server server2 = Server.builder(dummyServer())
				.addOutputWriterFactory(new DummyOutputWriterFactory("output 1"))
				.addOutputWriterFactory(new DummyOutputWriterFactory("output 3"))
				.build();

		ImmutableList<Server> serverList = new ServerListBuilder()
				.add(ImmutableList.of(server1, server2))
				.build();
		assertThat(serverList).hasSize(1);

		Server createdServer = serverList.iterator().next();
		assertThat(createdServer.getOutputWriters()).hasSize(3);
	}

	@Test
	public void outputWritersAreReusedOnQueries() {
		Query q1 = Query.builder(dummyQuery())
				.addOutputWriterFactory(new DummyOutputWriterFactory("output1"))
				.build();
		Query q2 = Query.builder(queryWithAllTypeNames())
				.addOutputWriterFactory(new DummyOutputWriterFactory("output1"))
				.build();
		Server server = Server.builder(serverWithNoQuery())
				.addQuery(q1)
				.addQuery(q2)
				.build();

		ImmutableList<Server> servers = new ServerListBuilder().add(singletonList(server)).build();

		assertThat(servers).hasSize(1);
		Server createdServer = servers.iterator().next();
		assertThat(createdServer.getQueries()).hasSize(2);

		Iterator<Query> queryIterator = createdServer.getQueries().iterator();
		Query query1 = queryIterator.next();
		Query query2 = queryIterator.next();

		assertThat(query1.getOutputWriterInstances()).hasSize(1);
		assertThat(query2.getOutputWriterInstances()).hasSize(1);

		assertThat(query1.getOutputWriterInstances().iterator().next())
				.isSameAs(query2.getOutputWriterInstances().iterator().next());
	}

	@Test
	public void outputWritersAreReusedOnServersAndQueries() {
		Server server = Server.builder(serverWithNoQuery())
				.addOutputWriterFactory(new DummyOutputWriterFactory("output1"))
				.addQuery(Query.builder(dummyQuery())
						.addOutputWriterFactory(new DummyOutputWriterFactory("output1"))
						.build())
				.build();

		ImmutableList<Server> servers = new ServerListBuilder().add(singletonList(server)).build();

		Server createdServer = servers.iterator().next();
		Query createdQuery = createdServer.getQueries().iterator().next();

		assertThat(createdQuery.getOutputWriterInstances()).hasSize(1);

		assertThat(createdServer.getOutputWriters().iterator().next())
				.isSameAs(createdQuery.getOutputWriterInstances().iterator().next());
	}

	@EqualsAndHashCode
	@ToString
	private static final class DummyOutputWriterFactory implements OutputWriterFactory {

		@Nonnull private final String name;

		private DummyOutputWriterFactory(@Nonnull String name) {
			this.name = name;
		}

		@Override
		public OutputWriter create() {
			return new OutputWriterAdapter() {
				@Override
				public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
				}
			};
		}
	}
}
