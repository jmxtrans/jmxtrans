package com.googlecode.jmxtrans.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilsTest {

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

}
