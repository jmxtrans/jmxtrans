package com.googlecode.jmxtrans.util;

import com.google.common.io.Closer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilsTest {

	@Test
	public void standardJsonPrinting() throws Exception {
		Closer closer = Closer.create();
		try {
			ByteArrayOutputStream baos = closer.register(new ByteArrayOutputStream());
			PrintStream out = closer.register(new PrintStream(baos));

			JsonUtils.printJson(standardProcess(), out);
			String result = new String(baos.toByteArray());

			assertThat(result).contains("\"url\":\"service:jmx:rmi:///jndi/rmi://example.org:123/jmxrmi\"");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@Test
	public void prettyJsonPrinting() throws Exception {
		Closer closer = Closer.create();
		try {
			ByteArrayOutputStream baos = closer.register(new ByteArrayOutputStream());
			PrintStream out = closer.register(new PrintStream(baos));

			JsonUtils.prettyPrintJson(standardProcess(), out);
			String result = new String(baos.toByteArray());

			assertThat(result).contains("\"url\" : \"service:jmx:rmi:///jndi/rmi://example.org:123/jmxrmi\"");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		File input = new File(JsonUtilsTest.class.getResource("/example.json").toURI());

		JmxProcess process = JsonUtils.getJmxProcess(input);
		assertThat(process.getName()).isEqualTo("example.json");

		Server server = process.getServers().get(0);
		assertThat(server.getNumQueryThreads()).isEqualTo(2);

		Query query = server.getQueries().get(0);
		assertThat(query.getAttr().get(0)).isEqualTo("HeapMemoryUsage");
	}

	private JmxProcess standardProcess() throws ValidationException {
		List<Server> servers = new ArrayList<Server>();
		Server server = new Server();
		server.setAlias("alias");
		server.setHost("example.org");
		server.setPort("123");
		Query query = new Query("obj");
		server.addQuery(query);
		servers.add(server);
		return new JmxProcess(servers);
	}
}
