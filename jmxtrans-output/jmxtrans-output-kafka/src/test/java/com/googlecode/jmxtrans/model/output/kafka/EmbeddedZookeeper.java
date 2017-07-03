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

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Embedded Zookeeper server for integration testing
 */
public class EmbeddedZookeeper extends ExternalResource {
	private final static Logger LOGGER = LoggerFactory.getLogger(EmbeddedZookeeper.class);
	private final ZooKeeperServerMain server = new ZooKeeperServerMain();
	private ServerCnxnFactory serverCnxnFactory;
	private final Executor executor = Executors.newSingleThreadExecutor();
	private final TemporaryFolder temporaryFolder;
	private File dataDir;

	public EmbeddedZookeeper(TemporaryFolder temporaryFolder) {
		this.temporaryFolder = temporaryFolder;
	}

	static InputStream getResourceAsStream(String name) throws FileNotFoundException {
		InputStream inputStream = EmbeddedZookeeper.class.getClassLoader().getResourceAsStream(name);
		if (inputStream == null) {
			throw new FileNotFoundException("Resource " + name + " not found");
		}
		return inputStream;
	}

	static Properties getResourceAsProperties(String name) throws IOException {
		try (InputStream inputStream = getResourceAsStream(name)) {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		}
	}

	@Override
	public void before() throws Exception {
		LOGGER.info("Starting Zookeeper");
		Properties properties = getResourceAsProperties("zookeeper.properties");
		dataDir = temporaryFolder.newFolder("zookeeper");
		properties.setProperty("dataDir", dataDir.getAbsolutePath());
		QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
		try {
			quorumConfiguration.parseProperties(properties);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		final ServerConfig configuration = new ServerConfig();
		configuration.readFrom(quorumConfiguration);
		// Start Zookeeper in separate thread
		executor.execute(new Runnable() {
			@Override
			public void run() {
				runServer(configuration);
			}
		});
		// Wait for Zookeeper to be started
		await().atMost(5, TimeUnit.SECONDS).until(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return getServerCnxnFactory() != null;
			}
		});
	}

	@Override
	public void after() {
		LOGGER.info("Stopping Zookeeper");
		ServerCnxnFactory serverCnxFactory = getServerCnxnFactory();
		if (serverCnxFactory != null) {
			serverCnxFactory.shutdown();
		}
	}

	/**
	 * Get hidden ServerCnxnFactory field through reflection
	 */
	private ServerCnxnFactory getServerCnxnFactory() {
		if (serverCnxnFactory != null) {
			return serverCnxnFactory;
		}
		try {
			Class<? extends ZooKeeperServerMain> serverClass = server.getClass();
			Field cnxnFactoryField = serverClass.getDeclaredField("cnxnFactory");
			if (!cnxnFactoryField.isAccessible()) {
				cnxnFactoryField.setAccessible(true);
			}
			Object o = cnxnFactoryField.get(server);
			if (o == null || o instanceof ServerCnxnFactory) {
				serverCnxnFactory = (ServerCnxnFactory) o;
				return serverCnxnFactory;
			}
			throw new RuntimeException("Invalid ServerCnxnFactory");
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void runServer(ServerConfig configuration) {
		try {
			server.runFromConfig(configuration);
		} catch (IOException e) {
			LOGGER.error("ZooKeeper Failed", e);
		}
	}

}
