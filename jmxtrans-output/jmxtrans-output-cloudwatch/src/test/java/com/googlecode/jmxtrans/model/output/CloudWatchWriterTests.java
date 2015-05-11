package com.googlecode.jmxtrans.model.output;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

	@Before
	public void createCloudWatchWriter() {
		writer = CloudWatchWriter.builder().setNamespace("testNS").build();
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
	}

}
