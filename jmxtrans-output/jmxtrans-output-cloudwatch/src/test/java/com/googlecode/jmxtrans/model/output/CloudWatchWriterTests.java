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

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
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
	private CloudWatchWriter.Writer writer;

	@Before
	public void createCloudWatchWriter() {
		ImmutableList<Dimension> dimensions = ImmutableList.of(
				new Dimension().withName("SomeKey").withValue("SomeValue"),
				new Dimension().withName("InstanceId").withValue("$InstanceId")
		);

		writer = new CloudWatchWriter.Writer("testNS", cloudWatchClient, dimensions);
	}

	@Test
	public void testValidationWithoutSettings() throws Exception {
		writer.doWrite(dummyServer(), dummyQuery(), dummyResults());
		verify(cloudWatchClient).putMetricData(requestCaptor.capture());

		PutMetricDataRequest request = requestCaptor.getValue();

		assertThat(request).isNotNull();
		assertThat(request.getNamespace()).isEqualTo("testNS");

		assertThat(request.getMetricData()).hasSize(1);
		MetricDatum metricDatum = request.getMetricData().get(0);
		assertThat(metricDatum.getMetricName()).isEqualTo("ObjectPendingFinalizationCount");
		assertThat(metricDatum.getValue()).isEqualTo(10);
		assertThat(metricDatum.getDimensions().size()).isEqualTo(2);
		assertThat(metricDatum.getDimensions().get(0).getName()).isEqualTo("SomeKey");
		assertThat(metricDatum.getDimensions().get(1).getName()).isEqualTo("InstanceId");
	}

}