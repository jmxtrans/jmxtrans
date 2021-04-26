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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.amazonaws.regions.Regions.AP_NORTHEAST_1;
import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Tests for {@link com.googlecode.jmxtrans.model.output.CloudWatchWriter}.
 *
 * @author <a href="mailto:sascha.moellering@gmail.com">Sascha Moellering</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DefaultAWSCredentialsProviderChain.class,DefaultAwsRegionProviderChain.class,AmazonCloudWatchClientBuilder.class,CloudWatchWriter.class})
public class CloudWatchWriterTests {

	@Mock AmazonCloudWatchClient cloudWatchClient;
	@Captor private ArgumentCaptor<PutMetricDataRequest> requestCaptor;
	private OutputWriter writer;

	@Before
	public void createCloudWatchWriter() throws Exception {

		mockStatic(DefaultAWSCredentialsProviderChain.class);

		DefaultAWSCredentialsProviderChain credentialsProviderChain = mock(DefaultAWSCredentialsProviderChain.class);
		DefaultAwsRegionProviderChain regionProviderChain = mock(DefaultAwsRegionProviderChain.class);

		when(credentialsProviderChain.getCredentials()).thenReturn(new BasicAWSCredentials("", ""));
		when(regionProviderChain.getRegion()).thenReturn(AP_NORTHEAST_1.getName());

		when(DefaultAWSCredentialsProviderChain.getInstance()).thenReturn(credentialsProviderChain);
		whenNew(DefaultAwsRegionProviderChain.class).withNoArguments().thenReturn(regionProviderChain);
		whenNew(AmazonCloudWatchClient.class).withAnyArguments().thenReturn(cloudWatchClient);



		CloudWatchWriter cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(),
				false,
				false,
				"testNS",
				Arrays.asList(
						(Map<String,Object>)ImmutableMap.of("name", (Object)"SomeKey", "value", (Object) "SomeValue"),
						(Map<String,Object>)ImmutableMap.of("name", (Object)"InstanceId",  "value", (Object) "$InstanceId")
				),
				ImmutableMap.<String, Object>of()
		);

		writer = cloudWatchWriter.create();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyNamespace() {
		CloudWatchWriter cloudWatchWriter = new CloudWatchWriter(
				ImmutableList.<String>of(),
				false,
				false,
				"",
				Arrays.asList(
						(Map<String,Object>)ImmutableMap.of("name", (Object)"SomeKey", "value", (Object) "SomeValue"),
						(Map<String,Object>)ImmutableMap.of("name", (Object)"InstanceId",  "value", (Object) "$InstanceId")
				),
				ImmutableMap.<String, Object>of()
		);
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

	@Test
	public void cloudwatchClientIsClosed() throws LifecycleException {
		writer.close();
		verify(cloudWatchClient).shutdown();
	}

}
