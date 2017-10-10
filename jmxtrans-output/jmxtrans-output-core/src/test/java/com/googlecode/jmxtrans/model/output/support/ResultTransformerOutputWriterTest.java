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
package com.googlecode.jmxtrans.model.output.support;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleFalseResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleTrueResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter.booleanToNumber;
import static com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter.identity;
import static java.lang.Boolean.FALSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ResultTransformerOutputWriterTest {

	@Mock private OutputWriter outputWriter;
	@Captor private ArgumentCaptor<ImmutableList<Result>> resultsCaptor;

	@Test
	public void booleanValuesAreTransformed() throws Exception {
		ResultTransformerOutputWriter<OutputWriter> resultTransformerOutputWriter = booleanToNumber(outputWriter);
		resultTransformerOutputWriter.doWrite(dummyServer(), dummyQuery(), singleTrueResult());

		verify(outputWriter)
				.doWrite(any(Server.class), any(Query.class), resultsCaptor.capture());
		assertThat(resultsCaptor.getValue()).hasSize(1);

		Result transformedResult = resultsCaptor.getValue().get(0);
		assertThat(transformedResult.getValue()).isEqualTo(1);
		assertThat(transformedResult.getValuePath()).isEmpty();
	}

	@Test
	public void identityTransformerDoesNotTransformValues() throws Exception {
		ResultTransformerOutputWriter<OutputWriter> resultTransformerOutputWriter = identity(outputWriter);
		resultTransformerOutputWriter.doWrite(dummyServer(), dummyQuery(), singleFalseResult());

		verify(outputWriter)
				.doWrite(any(Server.class), any(Query.class), resultsCaptor.capture());
		assertThat(resultsCaptor.getValue()).hasSize(1);

		Result transformedResult = resultsCaptor.getValue().get(0);
		assertThat(transformedResult.getValue()).isEqualTo(FALSE);
		assertThat(transformedResult.getValuePath()).isEmpty();
	}

}
