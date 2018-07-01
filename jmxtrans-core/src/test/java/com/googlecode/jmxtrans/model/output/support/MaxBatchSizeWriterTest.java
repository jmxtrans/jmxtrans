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

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Result;

@RunWith(MockitoJUnitRunner.class)
public class MaxBatchSizeWriterTest {

	@Mock private OutputWriter target;

	@Test
	public void resultsAreBatchedAccordingToMaxSize() throws Exception {
		MaxBatchSizeWriter<OutputWriter> batchWriter = new MaxBatchSizeWriter<>(2, target);
		ImmutableList<Result> results = ImmutableList.of(
				numericResult(1),
				numericResult(2),
				numericResult(3),
				numericResult(4),
				numericResult(5)
		);

		batchWriter.doWrite(dummyServer(), dummyQuery(), results);

		verify(target)
				.doWrite(dummyServer(), dummyQuery(), ImmutableList.of(
						numericResult(1),
						numericResult(2)));
		verify(target)
				.doWrite(dummyServer(), dummyQuery(), ImmutableList.of(
						numericResult(3),
						numericResult(4)));
		verify(target)
				.doWrite(dummyServer(), dummyQuery(), ImmutableList.of(
						numericResult(5)));

	}

}
