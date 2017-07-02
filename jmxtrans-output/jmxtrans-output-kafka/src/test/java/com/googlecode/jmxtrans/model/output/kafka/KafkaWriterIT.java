package com.googlecode.jmxtrans.model.output.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.test.MonitorableApp;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class KafkaWriterIT {
    @Rule public final MonitorableApp app = new MonitorableApp(12345);
    private final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final EmbeddedZookeeper zookeeper = new EmbeddedZookeeper(temporaryFolder);
    private final EmbeddedKafka kafka = new EmbeddedKafka(temporaryFolder);
    @Rule public final RuleChain zookeeperKafka = RuleChain.outerRule(temporaryFolder)
            .around(zookeeper)
            .around(kafka);

    @Before
    public void before() throws Exception {
        // Start JMXTrans
        JmxTransConfiguration configuration = new JmxTransConfiguration();
        configuration.setRunPeriod(1);
        configuration.setJsonFile(file("jmxtrans-kafka.json"));
        Injector injector = JmxTransModule.createInjector(configuration);
        jmxTransformer = injector.getInstance(JmxTransformer.class);
        jmxTransformer.start();
    }


    private JmxTransformer jmxTransformer;


    @Test
    public void testKafkaWriter() throws IOException {
        List<String> jmxtrans = kafka.consume("jmxtrans", "jmxtrans-kafka-it", 10000L);
        Assertions.assertThat(jmxtrans).isNotEmpty();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readValue(jmxtrans.get(0), JsonNode.class);

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
