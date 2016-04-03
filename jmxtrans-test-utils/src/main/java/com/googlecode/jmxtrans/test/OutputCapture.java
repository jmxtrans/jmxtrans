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
package com.googlecode.jmxtrans.test;

import org.junit.rules.ExternalResource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import static com.google.common.base.Charsets.UTF_8;

public class OutputCapture extends ExternalResource {

	private PrintStream originalOut;
	private PrintStream originalErr;
	private ByteArrayOutputStream out;
	private ByteArrayOutputStream err;

	@Override
	@SuppressWarnings("squid:S106") // capturing StdOut is the purpose of OutputCapture
	protected void before() throws Throwable {
		originalOut = System.out;
		originalErr = System.err;
		out = new ByteArrayOutputStream();
		System.setOut(new PrintStream(out, false, UTF_8.toString()));
		err = new ByteArrayOutputStream();
		System.setErr(new PrintStream(err, false, UTF_8.toString()));
	}

	@Override
	protected void after() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	public Callable<Boolean> stdoutHasLineContaining(final String content) {
		return hasLineContaining(out, content);
	}

	public Callable<Boolean> stderrHasLineContaining(final String content) {
		return hasLineContaining(err, content);
	}

	private Callable<Boolean> hasLineContaining(final ByteArrayOutputStream out, final String content) {
		return new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return out.toString(UTF_8.toString()).contains(content);
			}
		};
	}
}
