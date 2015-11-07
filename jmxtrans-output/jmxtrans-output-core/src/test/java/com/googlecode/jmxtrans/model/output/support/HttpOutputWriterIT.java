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
package com.googlecode.jmxtrans.model.output.support;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.test.IntegrationTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.addRequestProcessingDelay;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;

@Category(IntegrationTest.class)
public class HttpOutputWriterIT {

	private static final int WIREMOCK_PORT = 1234;
	public static final String ENDPOINT = "/endpoint";
	@Rule public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

	@Test
	public void messageIsSentOverHttp() throws Exception {
		stubFor(
				post(urlEqualTo(ENDPOINT))
						.willReturn(aResponse()
								.withBody("OK")
								.withStatus(200)));

		simpleOutputWriter().doWrite(dummyServer(), dummyQuery(), dummyResults());

		verify(
				postRequestedFor(urlEqualTo(ENDPOINT))
						.withRequestBody(equalTo("body"))
						.withHeader("User-Agent", containing("jmxtrans")));
	}

	@Test(expected = IOException.class)
	public void exceptionIsThrownOnServerError() throws Exception {
		stubFor(
				post(urlEqualTo(ENDPOINT))
						.willReturn(aResponse()
								.withBody("KO")
								.withStatus(500)));

		try {
			simpleOutputWriter().doWrite(dummyServer(), dummyQuery(), dummyResults());
		} finally {
			verify(
					postRequestedFor(urlEqualTo(ENDPOINT))
							.withRequestBody(equalTo("body"))
							.withHeader("User-Agent", containing("jmxtrans")));
		}
	}

	@Test(expected = SocketTimeoutException.class)
	public void socketTimeoutIsRespected() throws Exception {
		addRequestProcessingDelay(200);
		stubFor(
				post(urlEqualTo(ENDPOINT))
						.willReturn(aResponse()
								.withBody("OK")
								.withStatus(200)));

		simpleOutputWriter().doWrite(dummyServer(), dummyQuery(), dummyResults());
	}

	private HttpOutputWriter simpleOutputWriter() throws MalformedURLException {
		return new HttpOutputWriter(
				new WriterBasedOutputWriter() {
					@Override
					public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull ImmutableList<Result> results) throws IOException {
						writer.write("body");
					}
				},
				new URL("http://localhost:" + WIREMOCK_PORT + ENDPOINT),
				null,
				HttpUrlConnectionConfigurer.builder("POST").build(),
				Charsets.UTF_8
		);
	}
}
