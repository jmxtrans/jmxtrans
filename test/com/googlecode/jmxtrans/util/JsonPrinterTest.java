package com.googlecode.jmxtrans.util;

import com.google.common.io.Closer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonPrinterTest {

	@Test
	public void standardJsonPrinting() throws Exception {
		Closer closer = Closer.create();
		try {
			ByteArrayOutputStream baos = closer.register(new ByteArrayOutputStream());
			PrintStream out = closer.register(new PrintStream(baos));

			new JsonPrinter(out).print(standardProcess());
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

			new JsonPrinter(out).prettyPrint(standardProcess());
			String result = new String(baos.toByteArray());

			assertThat(result).contains("\"url\" : \"service:jmx:rmi:///jndi/rmi://example.org:123/jmxrmi\"");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	private JmxProcess standardProcess() throws ValidationException {
		List<Server> servers = new ArrayList<Server>();
		Server server = new Server();
		server.setAlias("alias");
		server.setHost("example.org");
		server.setPort("123");
		Query query = Query.builder()
				.setObj("obj")
				.build();
		server.addQuery(query);
		servers.add(server);
		return new JmxProcess(servers);
	}

}
