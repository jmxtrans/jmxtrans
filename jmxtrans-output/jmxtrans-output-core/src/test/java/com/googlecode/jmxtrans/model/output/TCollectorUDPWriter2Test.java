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

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ServerFixtures;
import com.googlecode.jmxtrans.model.output.support.opentsdb.OpenTSDBMessageFormatter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class TCollectorUDPWriter2Test {

	private OpenTSDBMessageFormatter openTSDBMessageFormatter;
	private TCollectorUDPWriter2 writer;
	private Writer outputWriter;
	private Result result;

	@Before
	public void setup() {
		openTSDBMessageFormatter = Mockito.mock(OpenTSDBMessageFormatter.class);
		writer = new TCollectorUDPWriter2(openTSDBMessageFormatter);
		outputWriter = Mockito.mock(Writer.class);
		result = Mockito.mock(Result.class);
	}

	@Test
	public void testMultipleSend() throws IOException {

		ImmutableList<Result> results = ImmutableList.of(result, result);
		List<String> resultsString = ImmutableList.of("Result1", "Result2");

		Mockito.when(openTSDBMessageFormatter.formatResults(results, ServerFixtures.dummyServer())).thenReturn(resultsString);

		writer.write(outputWriter, ServerFixtures.dummyServer(), null, results);

		Mockito.verify(outputWriter).write("Result1");
		Mockito.verify(outputWriter).write("Result2");
	}
}