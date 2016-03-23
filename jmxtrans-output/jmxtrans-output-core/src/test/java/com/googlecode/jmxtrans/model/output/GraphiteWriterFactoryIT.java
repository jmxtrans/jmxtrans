package com.googlecode.jmxtrans.model.output;

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
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.test.ResetableSystemProperty;
import com.kaching.platform.testing.AllowDNSResolution;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

@Category({IntegrationTest.class, RequiresIO.class})
@AllowDNSResolution
public class GraphiteWriterFactoryIT {

	private ConfigurationParser configurationParser;
	private final Closer closer = Closer.create();

	@Before
	public void createConfigurationParser() {
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		Injector injector = JmxTransModule.createInjector(configuration);
		configurationParser = injector.getInstance(ConfigurationParser.class);
	}

	@Before
	public void setupProperties() {
		closer.register(ResetableSystemProperty.setSystemProperty("server.port", "1099"));
		closer.register(ResetableSystemProperty.setSystemProperty("server.attribute", "HeapMemoryUsage"));
		closer.register(ResetableSystemProperty.setSystemProperty("server.thread", "2"));
		closer.register(ResetableSystemProperty.setSystemProperty("graphite.host", "example.net"));
	}

	@After
	public void resetProperties() throws IOException {
		closer.close();
	}

	@Test
	public void canParseConfigurationFile() throws LifecycleException, URISyntaxException {
		ImmutableList<Server> servers = configurationParser.parseServers(ImmutableList.of(file("/graphite-writer-factory-example.json")), false);

		assertThat(servers).hasSize(1);
		Server server = servers.get(0);

		assertThat(server.getNumQueryThreads()).isEqualTo(2);

		assertThat(server.getQueries()).hasSize(1);
		Query query = server.getQueries().iterator().next();

		assertThat(query.getOutputWriterInstances()).hasSize(1);
		OutputWriter outputWriter = query.getOutputWriterInstances().iterator().next();

		assertThat(outputWriter).isInstanceOf(ResultTransformerOutputWriter.class);
	}

	private File file(String filename) throws URISyntaxException {
		return new File(GraphiteWriterFactoryIT.class.getResource(filename).toURI());
	}

}
