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
package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.test.DummyApp;
import com.googlecode.jmxtrans.test.ExternalApp;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Category({IntegrationTest.class, RequiresIO.class})
public class KafkaWriterIT {
	@Rule
	public final ExternalApp app = new ExternalApp(DummyApp.class).enableJmx(12347);
	private final TemporaryFolder temporaryFolder = new TemporaryFolder();
	private final EmbeddedZookeeper zookeeper = new EmbeddedZookeeper(temporaryFolder);
	private final EmbeddedKafka kafka = new EmbeddedKafka(temporaryFolder);
	@Rule
	public final RuleChain zookeeperKafka = RuleChain.outerRule(temporaryFolder)
			.around(zookeeper)
			.around(kafka);

	@Before
	public void before() throws Exception {
		// Start JMXTrans
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		configuration.setRunPeriod(1);
		configuration.setProcessConfigFile(file("jmxtrans-kafka.json"));
		Injector injector = JmxTransModule.createInjector(configuration);
		jmxTransformer = injector.getInstance(JmxTransformer.class);
		jmxTransformer.start();
	}


	private JmxTransformer jmxTransformer;


	@Test
	public void testKafkaWriter() throws IOException {
		List<String> messages = kafka.consume("jmxtrans", "jmxtrans-kafka-it", 10000L);
		assertThat(messages).isNotEmpty();
		ObjectMapper objectMapper = new ObjectMapper();
		for(String message: messages) {
			JsonNode jsonNode = objectMapper.readValue(message, JsonNode.class);
			assertThat(jsonNode.get("keyspace").asText()).startsWith("test.localhost_12347.");
			assertThat(jsonNode.get("value").asText()).isNotEmpty();
			assertThat(jsonNode.get("timestamp").asLong()).isCloseTo(System.currentTimeMillis() / 1000L, within(10L));
		}
	}

	@Before
	public void startJmxTrans() throws LifecycleException, URISyntaxException {
	}

	private File file(String filename) throws URISyntaxException {
		return new File(getClass().getClassLoader().getResource(filename).toURI());
	}

	@After
	public void after() throws Exception {
		jmxTransformer.stop();
	}
}
