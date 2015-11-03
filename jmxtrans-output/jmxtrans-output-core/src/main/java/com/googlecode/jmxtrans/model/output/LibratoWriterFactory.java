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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.output.support.HttpOutputWriter;
import com.googlecode.jmxtrans.model.output.support.HttpUrlConnectionConfigurer;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static com.google.common.base.Charsets.US_ASCII;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

public class LibratoWriterFactory implements OutputWriterFactory {
	private final boolean booleanAsNumber;
	@Nonnull private final ImmutableList<String> typeNames;
	@Nonnull private final URL url;
	@Nullable private final Proxy proxy;
	@Nonnull private final int readTimeoutInMillis;
	@Nullable private final String authorization;

	public LibratoWriterFactory(
			@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
			@JsonProperty("url") URL url,
			@JsonProperty("libratoApiTimeoutInMillis") Integer readTimeoutInMillis,
			@JsonProperty("username") String username,
			@JsonProperty("token") String token,
			@JsonProperty("proxyHost") String proxyHost,
			@JsonProperty("proxyPort") Integer proxyPort) {
		this.booleanAsNumber = booleanAsNumber;
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
		this.url = checkNotNull(url);
		this.readTimeoutInMillis = firstNonNull(readTimeoutInMillis, 1000);
		this.authorization = "Basic" + Base64Variants
				.getDefaultVariant()
				.encode(
						(checkNotNull(username) + ":" + checkNotNull(token)).getBytes(US_ASCII)
				);
		proxy = initProxy(proxyHost, proxyPort);
	}

	private Proxy initProxy(String proxyHost, Integer proxyPort) {
		if (proxyHost == null) return null;

		return new Proxy(
				Proxy.Type.HTTP,
				new InetSocketAddress(
						proxyHost,
						checkNotNull(proxyPort, "Proxy port needs to be specified.")));
	}

	@Override
	public OutputWriter create() {
		return ResultTransformerOutputWriter.booleanToNumber(
				booleanAsNumber,
				new HttpOutputWriter<LibratoWriter2>(
						new LibratoWriter2(
								new JsonFactory(),
								typeNames),
						url,
						proxy,
						new HttpUrlConnectionConfigurer(
								"POST",
								readTimeoutInMillis,
								authorization,
								"application/json; charset=utf-8"
						),
						UTF_8
				));
	}
}
