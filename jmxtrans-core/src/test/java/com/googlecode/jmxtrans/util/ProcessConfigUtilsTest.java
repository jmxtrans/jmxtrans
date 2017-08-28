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
package com.googlecode.jmxtrans.util;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.Closer;
import com.google.inject.Injector;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.guice.JmxTransModule;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.test.ResetableSystemProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.Nullable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresIO.class)
public class ProcessConfigUtilsTest {

	private ProcessConfigUtils processConfigUtils;
	private Closer closer = Closer.create();

	@Before
	public void setupJsonUtils() {
		Injector injector = JmxTransModule.createInjector(new JmxTransConfiguration());
		processConfigUtils = injector.getInstance(ProcessConfigUtils.class);

		closer.register(ResetableSystemProperty.setSystemProperty("server.port", "1099"));
		closer.register(ResetableSystemProperty.setSystemProperty("server.attribute", "HeapMemoryUsage"));
		closer.register(ResetableSystemProperty.setSystemProperty("server.thread", "2"));
	}

	@After
	public void cleanUpVariables() throws IOException {
		closer.close();
	}

	@Test
	public void loadingFromSimpleJsonFile() throws URISyntaxException, IOException, MalformedObjectNameException {
		loadFromFile("example.json");
	}

	@Test
	public void loadingFromSimpleYamlFile() throws URISyntaxException, IOException, MalformedObjectNameException {
		loadFromFile("example.yaml");
	}

	@Test
	public void loadingFromJsonFileWithVariables() throws Exception {
		loadFromFile("exampleWithVariables.json");
	}

	@Test
	public void loadingFromYamlFileWithVariables() throws Exception {
		loadFromFile("exampleWithVariables.yaml");
	}

	private void loadFromFile(String file) throws URISyntaxException, IOException, MalformedObjectNameException {
		File input = new File(ProcessConfigUtilsTest.class.getResource("/" + file).toURI());

		JmxProcess process = processConfigUtils.parseProcess(input);
		assertThat(process.getName()).isEqualTo(file);

		Server server = process.getServers().get(0);
		assertThat(server.getPort()).isEqualTo("1099");
		assertThat(server.getNumQueryThreads()).isEqualTo(2);

		Optional<Query> queryOptional = from(server.getQueries()).firstMatch(new ByObj("java.lang:type=Memory"));
		assertThat(queryOptional.isPresent()).isTrue();
		assertThat(queryOptional.get().getAttr().get(0)).isEqualTo("HeapMemoryUsage");
	}

	private static class ByObj implements Predicate<Query> {

		private final ObjectName obj;

		private ByObj(String obj) throws MalformedObjectNameException {
			this.obj = new ObjectName(obj);
		}

		@Override
		public boolean apply(@Nullable Query query) {
			return query.getObjectName().equals(this.obj);}
	}
}
