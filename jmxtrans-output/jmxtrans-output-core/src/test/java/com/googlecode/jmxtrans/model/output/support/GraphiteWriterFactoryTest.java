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
package com.googlecode.jmxtrans.model.output.support;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.ConfigurationParser;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.model.output.GraphiteWriter2;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.test.ResetableSystemProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

@Category({IntegrationTest.class, RequiresIO.class})
public class GraphiteWriterFactoryTest {

	private ConfigurationParser configurationParser;

	@Before
	public void createConfigurationParser() {
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		Injector injector = JmxTransModule.createInjector(configuration);
		configurationParser = injector.getInstance(ConfigurationParser.class);
	}

	@Test
	public void canParseConfigurationFileWithCustomParameters() throws LifecycleException, URISyntaxException {
		ImmutableList<Server> servers = configurationParser.parseServers(ImmutableList.of(file("/graphite-writer-factory-example2.json")), false);

		assertThat(servers).hasSize(1);
		Server server = servers.get(0);

		assertThat(server.getQueries()).hasSize(1);
		Query query = server.getQueries().iterator().next();

		assertThat(query.getOutputWriterInstances()).hasSize(1);
		OutputWriter outputWriter = query.getOutputWriterInstances().iterator().next();

		assertThat(outputWriter).isInstanceOf(ResultTransformerOutputWriter.class);

		ResultTransformerOutputWriter resultTransformerOutputWriter = (ResultTransformerOutputWriter) outputWriter;
		OutputWriter target = resultTransformerOutputWriter.getTarget();

		assertThat(target).isInstanceOf(WriterPoolOutputWriter.class);

		WriterPoolOutputWriter writerPoolOutputWriter = (WriterPoolOutputWriter) target;

		assertThat(writerPoolOutputWriter.getSocketTimeoutMs()).isEqualTo(1000);
		assertThat(writerPoolOutputWriter.getPoolClaimTimeout().getTimeout()).isEqualTo(2);
	}

	private File file(String filename) throws URISyntaxException {
		return new File(GraphiteWriterFactoryTest.class.getResource(filename).toURI());
	}

}
