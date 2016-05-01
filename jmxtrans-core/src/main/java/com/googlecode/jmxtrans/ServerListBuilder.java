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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.SingletonOutputWriterFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

@NotThreadSafe
public class ServerListBuilder {

	@Nonnull private final Map<Server, TemporaryServer> servers = newHashMap();
	@Nonnull private final Map<OutputWriterFactory, OutputWriterFactory> outputWriters = newHashMap();

	public ServerListBuilder add(Iterable<Server> servers) {
		for (Server server : servers) {
			add(server);
		}
		return this;
	}

	private void add(Server server) {
		TemporaryServer temporaryServer = singleton(server);
		temporaryServer.addQueries(server.getQueries());
		temporaryServer.addOutputWriters(server.getOutputWriterFactories());
	}

	private TemporaryServer singleton(Server server) {
		if (!servers.containsKey(server)) servers.put(server, new TemporaryServer(server));
		return servers.get(server);
	}

	private OutputWriterFactory singleton(OutputWriterFactory outputWriter) {
		outputWriter = new SingletonOutputWriterFactory(outputWriter);
		if (!outputWriters.containsKey(outputWriter)) outputWriters.put(outputWriter, outputWriter);
		return outputWriters.get(outputWriter);
	}

	public ImmutableList<Server> build() {
		ImmutableList.Builder<Server> builder = ImmutableList.builder();
		for (TemporaryServer server : servers.values()) {
			builder.add(server.build());
		}
		return builder.build();
	}

	private class TemporaryServer {
		@Nonnull private final Server server;
		@Nonnull private final Map<Query, Set<OutputWriterFactory>> queries = newHashMap();
		@Nonnull private final Set<OutputWriterFactory> temporaryOutputWriters = newHashSet();

		TemporaryServer(Server server) {
			this.server = server;
		}

		public void addQueries(Iterable<Query> queries) {
			for (Query query : queries) {
				addQuery(query);
			}
		}

		private void addQuery(Query query) {
			if (!queries.containsKey(query)) queries.put(query, new HashSet<OutputWriterFactory>());

			Set<OutputWriterFactory> outputWritersForThisQuery = queries.get(query);
			for (OutputWriterFactory outputWriter : query.getOutputWriters()) {
				outputWritersForThisQuery.add(singleton(outputWriter));
			}
		}

		public void addOutputWriters(Iterable<OutputWriterFactory> outputWriters) {
			for (OutputWriterFactory outputWriter : outputWriters) {
				temporaryOutputWriters.add(singleton(outputWriter));
			}
		}

		public Server build() {
			Server.Builder builder = Server.builder(server)
					.addOutputWriters(createOutputWriters(temporaryOutputWriters))
					.clearQueries();
			for (Map.Entry<Query, Set<OutputWriterFactory>> queryEntry : queries.entrySet()) {
				builder.addQuery(
						Query.builder(queryEntry.getKey())
								.addOutputWriters(createOutputWriters(queryEntry.getValue()))
								.build());
			}
			return builder.build();
		}

	}

	private Collection<OutputWriter> createOutputWriters(Set<OutputWriterFactory> outputWriterFactories) {
		return from(outputWriterFactories)
				.transform(new Function<OutputWriterFactory, OutputWriter>() {
			@Nullable
			@Override
			public OutputWriter apply(OutputWriterFactory input) {
				return input.create();
			}
		}).toList();
	}
}
