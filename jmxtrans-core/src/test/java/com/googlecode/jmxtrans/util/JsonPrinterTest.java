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
package com.googlecode.jmxtrans.util;

import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonPrinterTest {

	@Test
	public void standardJsonPrinting() throws Exception {
		Closer closer = Closer.create();
		try {
			ByteArrayOutputStream baos = closer.register(new ByteArrayOutputStream());
			PrintStream out = closer.register(new PrintStream(baos));

			new JsonPrinter(out).print(standardProcess());
			String result = new String(baos.toByteArray());

			assertThat(result).contains("\"url\":\"service:jmx:rmi:///jndi/rmi://example.org:123/jmxrmi\"");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	@Test
	public void prettyJsonPrinting() throws Exception {
		Closer closer = Closer.create();
		try {
			ByteArrayOutputStream baos = closer.register(new ByteArrayOutputStream());
			PrintStream out = closer.register(new PrintStream(baos));

			new JsonPrinter(out).prettyPrint(standardProcess());
			String result = new String(baos.toByteArray());

			assertThat(result).contains("\"url\" : \"service:jmx:rmi:///jndi/rmi://example.org:123/jmxrmi\"");
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	private JmxProcess standardProcess() {
		Server server = Server.builder()
				.setAlias("alias")
				.setHost("example.org")
				.setPort("123")
				.addQuery(Query.builder()
					.setObj("obj")
					.build())
				.build();
		return new JmxProcess(server);
	}

}
