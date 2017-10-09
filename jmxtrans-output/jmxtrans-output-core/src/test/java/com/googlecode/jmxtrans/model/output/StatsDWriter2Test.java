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
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.*;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

public class StatsDWriter2Test {

	@Test
	public void writeNumericResult() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.<String>of(), "root", "c", false, 1L, "_");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), singleNumericResult());

		assertThat(out.toString())
				.isEqualTo("root.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount:10|c\n");
	}

	@Test
	public void valuesTruncatedToCPrecision() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.<String>of(), "root", "c", false, 1L, "_");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), singleNumericBelowCPrecisionResult());

		assertThat(out.toString())
				.isEqualTo("root.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount:0|c\n");
	}

	@Test
	public void ignoreNonNumericValues() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.<String>of(), "root", "c", false, 1L, "_");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), singleTrueResult());

		assertThat(out.toString()).isEmpty();
	}

	@Test
	public void handleNaNValues() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.<String>of(), "root", "g", true, 1L, "_");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), singleResult(numericResult(Double.NaN)));

		assertThat(out.toString())
				.isEqualTo("root.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount:|g\n");
	}

	@Test
	public void nonNumericValuesAsKey() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.<String>of(), "root", "g", true, 1L, "_");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), singleTrueResult());

		assertThat(out.toString())
				.isEqualTo("root.host_example_net_4321.VerboseMemory.Verbose.true:1|g\n");
	}

	@Test
	public void multipleValuesAreSeparatedByNewLine() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.<String>of(), "root", "g", true, 1L, "_");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), dummyResults());

		assertThat(out.toString())
				.isEqualTo("root.host_example_net_4321.MemoryAlias.ObjectPendingFinalizationCount:10|g\n" +
						"root.host_example_net_4321.VerboseMemory.Verbose.true:1|g\n" +
						"root.host_example_net_4321.VerboseMemory.Verbose.false:1|g\n");
	}

	@Test
	public void badKeyNameNoInvalidCharProvided() throws IOException {
		StatsDWriter2 writer = new StatsDWriter2(ImmutableList.of("scope", "name"), "root", "g", true, 1L, "___");

		StringWriter out = new StringWriter();
		writer.write(out, dummyServer(), dummyQuery(), dummyResultWithColon());

		assertThat(out.toString())
				.isEqualTo("root.host_example_net_4321.com_yammer_metrics_reporting_JmxReporter$Meter.127_0_0_1___8008_4XX.Count:10|g\n" +
						"root.host_example_net_4321.VerboseMemory.Verbose.true:1|g\n");
	}

}
