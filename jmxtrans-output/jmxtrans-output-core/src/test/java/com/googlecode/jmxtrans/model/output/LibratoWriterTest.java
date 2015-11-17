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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class LibratoWriterTest {

	@Test
	public void httpUserAgentContainsAppropriateInformation() throws MalformedURLException {
		LibratoWriter writer = new LibratoWriter(
				ImmutableList.<String>of(),
				false,
				false,
				new URL(LibratoWriter.DEFAULT_LIBRATO_API_URL),
				1000,
				"username",
				"token",
				null,
				null,
				ImmutableMap.<String, Object>of()
		);

		Assertions.assertThat(writer.httpUserAgent)
				.startsWith("jmxtrans-standalone/")
				.contains(System.getProperty("os.name"))
				.contains(System.getProperty("os.arch"))
				.contains(System.getProperty("os.version"))
				.contains(System.getProperty("java.vm.name"))
				.contains(System.getProperty("java.version"));
	}
	

}
