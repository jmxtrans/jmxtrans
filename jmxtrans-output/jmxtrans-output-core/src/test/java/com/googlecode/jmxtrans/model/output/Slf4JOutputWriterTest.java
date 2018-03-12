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

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleNumericResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.stringResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class Slf4JOutputWriterTest {

	private Slf4JOutputWriter outputWriter;

	@Mock private Logger logger;

	@Mock private ResultSerializer resultSerializer;

	@Before
	public void initOutputWriter() {
		outputWriter = new Slf4JOutputWriter(logger, resultSerializer);
	}

	@Test
	public void metricsAreSentToLoggerViaMDC() throws Exception {
		when(resultSerializer.serialize(any(Server.class), any(Query.class), any(Result.class)))
				.thenReturn("");
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) {
				assertThat(MDC.get("server")).isEqualTo("host_example_net_4321");
				assertThat(MDC.get("metric")).isEqualTo("host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount");
				assertThat(MDC.get("value")).isEqualTo("10");
				assertThat(MDC.get("attributeName")).isEqualTo("ObjectPendingFinalizationCount");
				assertThat(MDC.get("key")).isEqualTo("ObjectPendingFinalizationCount");
				assertThat(MDC.get("epoch")).isEqualTo("0");
				return null;
			}
		}).when(logger).info("");

		outputWriter.doWrite(dummyServer(), dummyQuery(), singleNumericResult());
	}

	@Test
	public void mdcIsCleanedAfterCall() throws Exception {
		when(resultSerializer.serialize(any(Server.class), any(Query.class), any(Result.class)))
				.thenReturn("");
		outputWriter.doWrite(dummyServer(), dummyQuery(), singleNumericResult());

		assertThat(MDC.get("server")).isNull();
		assertThat(MDC.get("metric")).isNull();
		assertThat(MDC.get("value")).isNull();
		assertThat(MDC.get("attributeName")).isNull();
		assertThat(MDC.get("key")).isNull();
		assertThat(MDC.get("epoch")).isNull();
	}

	@Test
	public void nonNumericMetricsAreNotLoggedByDefault() throws Exception {
		outputWriter = new Slf4JOutputWriter(logger, null);

		outputWriter.doWrite(dummyServer(), dummyQuery(), singleResult(stringResult()));

		verifyNoMoreInteractions(logger);
	}

	@Test
	public void numericMetricsAreEmptyByDefault() throws Exception {
		outputWriter = new Slf4JOutputWriter(logger, null);

		outputWriter.doWrite(dummyServer(), dummyQuery(), singleNumericResult());

		verify(logger).info(eq(""));
	}

	@Test
	public void resultSerializerFiltersLogs() throws Exception {
		when(resultSerializer.serialize(any(Server.class), any(Query.class), any(Result.class)))
				.thenReturn(null);
		outputWriter.doWrite(dummyServer(), dummyQuery(), singleNumericResult());
		verifyNoMoreInteractions(logger);
	}
}
