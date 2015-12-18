/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.util.JsonUtils;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link com.googlecode.jmxtrans.model.output.CloudWatchWriter}.
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudWatchWriterTests {

	@Mock private AmazonCloudWatchClient cloudWatchClient;
	@Captor private ArgumentCaptor<PutMetricDataRequest> requestCaptor;
	private CloudWatchWriter writer;

	private Server server;
	private Query query;
	private ImmutableList<Result> results;

	private Map<String, Object> createValidDimension(String name, Object value) {
		Map<String, Object> returnValue = new HashMap<String, Object>();
		returnValue.put("name", name);
		returnValue.put("value", value);
		return returnValue;
	}

	private Map<String, Object> createInvalidDimension(String name, Object value) {
		Map<String, Object> returnValue = new HashMap<String, Object>();
		returnValue.put("key", name);
		returnValue.put("val", value);
		return returnValue;
	}

	@Before
	public void createCloudWatchWriter() {
		Collection<Map<String, Object>> dimensions = new ArrayList<Map<String,Object>>();
		dimensions.add(createValidDimension("SomeKey", "SomeValue"));
		dimensions.add(createValidDimension("InstanceId", "$InstanceId"));
		dimensions.add(createInvalidDimension("Other", "thing"));

		writer = CloudWatchWriter.builder().setNamespace("testNS").setDimensions(dimensions).build();
		writer.setCloudWatchClient(cloudWatchClient);
	}

	@Before
	public void createServerQueryAndResult() {
		server = Server.builder().setHost("localhost").setPort("123").build();
		query = Query.builder()
				.setObj("test")
				.build();
		Result result = new Result(1, "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object) 1));
		results = ImmutableList.of(result);
	}

	@Test
	public void testValidationWithoutSettings() throws Exception {
		writer.doWrite(server, query, results);
		verify(cloudWatchClient).putMetricData(requestCaptor.capture());

		PutMetricDataRequest request = requestCaptor.getValue();

		assertThat(request).isNotNull();
		assertThat(request.getNamespace()).isEqualTo("testNS");

		assertThat(request.getMetricData()).hasSize(1);
		MetricDatum metricDatum = request.getMetricData().get(0);
		assertThat(metricDatum.getMetricName()).isEqualTo("attributeName_key");
		assertThat(metricDatum.getValue()).isEqualTo(1);
		assertThat(metricDatum.getDimensions().size()).isEqualTo(2);
		assertThat(metricDatum.getDimensions().get(0).getName()).isEqualTo("SomeKey");
		assertThat(metricDatum.getDimensions().get(1).getName()).isEqualTo("InstanceId");
	}

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		File input = new File(CloudWatchWriterTests.class.getResource("/cloud-watch.json").toURI());
		JmxProcess process = JsonUtils.getJmxProcess(input);
		assertThat(process.getName()).isEqualTo("cloud-watch.json");
	}

}