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
package com.googlecode.jmxtrans.model.output;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.cli.JmxTransConfiguration;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.test.IntegrationTest;
import com.googlecode.jmxtrans.test.RequiresIO;
import com.googlecode.jmxtrans.util.ProcessConfigUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static com.amazonaws.regions.Region.getRegion;
import static com.amazonaws.regions.Regions.AP_NORTHEAST_1;
import static com.googlecode.jmxtrans.guice.JmxTransModule.createInjector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Tests for {@link CloudWatchWriterFactory}.
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
@Category({RequiresIO.class, IntegrationTest.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({CloudWatchWriter2.class, Regions.class})
public class CloudWatchWriterIT {

	@Mock
	private AmazonCloudWatchClient cloudWatchClient;

	@Before
	public void mockAmazonAPI() throws Exception {
		whenNew(AmazonCloudWatchClient.class)
				.withAnyArguments()
				.thenReturn(cloudWatchClient);

		mockStatic(Regions.class);
		when(Regions.getCurrentRegion()).thenReturn(getRegion(AP_NORTHEAST_1));
	}

	@Test
	public void testReadConfigDefault() throws IOException, URISyntaxException {
		ProcessConfigUtils processConfigUtils = createInjector(new JmxTransConfiguration()).getInstance(
				ProcessConfigUtils.class);
		File input = new File(CloudWatchWriterIT.class.getResource("/cloud-watch.json").toURI());
		JmxProcess process = processConfigUtils.parseProcess(input);
		assertThat(process.getName()).isEqualTo("cloud-watch.json");

		CloudWatchWriterFactory writerFactory = (CloudWatchWriterFactory) process.getServers()
				.get(0).getQueries().asList().get(0).getOutputWriters().get(0);
		assertThat(writerFactory.getNamespace()).isEqualTo("jmx");
		assertThat(writerFactory.getTypeNames()).isNull();
		assertThat(writerFactory.isBooleanAsNumber()).isFalse();

		ImmutableList<Map<String, Object>> dimensions = FluentIterable.from(writerFactory.getDimensions()).toList();
		assertThat(dimensions.get(0).get("name")).isEqualTo("InstanceId");
		assertThat(dimensions.get(0).get("value")).isEqualTo("$InstanceId");

		assertThat(dimensions.get(1).get("name")).isEqualTo("SomeKey");
		assertThat(dimensions.get(1).get("value")).isEqualTo("SomeValue");
	}

	@Test
	public void testReadConfigWithTypeName() throws IOException, URISyntaxException {
		ProcessConfigUtils processConfigUtils = createInjector(new JmxTransConfiguration()).getInstance(
				ProcessConfigUtils.class);
		File input = new File(CloudWatchWriterIT.class.getResource("/cloud-watch-config-typename.json").toURI());
		JmxProcess process = processConfigUtils.parseProcess(input);
		assertThat(process.getName()).isEqualTo("cloud-watch-config-typename.json");

		CloudWatchWriterFactory writerFactory = (CloudWatchWriterFactory) process.getServers()
				.get(0).getQueries().asList().get(0).getOutputWriters().get(0);
		assertThat(writerFactory.getNamespace()).isEqualTo("jmx");
		assertThat(writerFactory.getTypeNames().size()).isEqualTo(1);
		assertThat(writerFactory.getTypeNames().get(0)).isEqualTo("type");
		assertThat(writerFactory.isBooleanAsNumber()).isFalse();

		ImmutableList<Map<String, Object>> dimensions = FluentIterable.from(writerFactory.getDimensions()).toList();
		assertThat(dimensions.get(0).get("name")).isEqualTo("InstanceId");
		assertThat(dimensions.get(0).get("value")).isEqualTo("$InstanceId");

		assertThat(dimensions.get(1).get("name")).isEqualTo("SomeKey");
		assertThat(dimensions.get(1).get("value")).isEqualTo("SomeValue");
	}

	@Test
	public void testReadDeprecatedConfig() throws URISyntaxException, IOException {
		ProcessConfigUtils processConfigUtils = createInjector(new JmxTransConfiguration()).getInstance(
				ProcessConfigUtils.class);
		File input = new File(CloudWatchWriterIT.class.getResource("/cloud-watch-deprecated.json").toURI());
		JmxProcess process = processConfigUtils.parseProcess(input);
		assertThat(process.getName()).isEqualTo("cloud-watch-deprecated.json");

		CloudWatchWriterFactory writerFactory = ((CloudWatchWriter) process.getServers()
				.get(0).getQueries().asList().get(0).getOutputWriters().get(0)).getCloudWatchWriterFactory();
		assertThat(writerFactory.getNamespace()).isEqualTo("jmx");
		assertThat(writerFactory.getTypeNames()).isNull();
		assertThat(writerFactory.isBooleanAsNumber()).isFalse();

		ImmutableList<Map<String, Object>> dimensions = FluentIterable.from(writerFactory.getDimensions()).toList();
		assertThat(dimensions.get(0).get("name")).isEqualTo("InstanceId");
		assertThat(dimensions.get(0).get("value")).isEqualTo("$InstanceId");

		assertThat(dimensions.get(1).get("name")).isEqualTo("SomeKey");
		assertThat(dimensions.get(1).get("value")).isEqualTo("SomeValue");
	}
}
