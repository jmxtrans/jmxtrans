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

import com.google.common.io.Closer;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;

public class HttpOutputWriter<T extends WriterBasedOutputWriter> extends OutputWriterAdapter {

	@Nonnull private final Logger logger = LoggerFactory.getLogger(HttpOutputWriter.class);

	@Nonnull private final T target;
	@Nonnull private final URL url;
	@Nullable private final Proxy proxy;
	@Nonnull private final HttpUrlConnectionConfigurer configurer;
	@Nonnull private final Charset charset;

	public HttpOutputWriter(
			@Nonnull T target,
			@Nonnull URL url,
			@Nullable Proxy proxy,
			@Nonnull HttpUrlConnectionConfigurer configurer,
			@Nullable Charset charset) {
		this.target = target;
		this.url = url;
		this.proxy = proxy;
		this.configurer = configurer;
		this.charset = firstNonNull(charset, UTF_8);
	}

	@Override
	public void doWrite(Server server, Query query, Iterable<Result> results) throws IOException {
		HttpURLConnection httpURLConnection = createHttpURLConnection();
		try {
			configurer.configure(httpURLConnection);

			writeResults(server, query, results, httpURLConnection);

			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode != 200) {
				logger.warn(
						"Failure {}:'{}' to send result to server '{}' with proxy {}",
						responseCode, httpURLConnection.getResponseMessage(), url, proxy);
			}
			if (logger.isTraceEnabled()) {
				logger.trace(IOUtils.toString(httpURLConnection.getInputStream()));
			}
		} finally {
			consumeInputStreams(httpURLConnection);
		}
	}

	private void consumeInputStreams(HttpURLConnection httpURLConnection) throws IOException {
		Closer closer = Closer.create();
		try {
			InputStream in = closer.register(httpURLConnection.getInputStream());
			InputStream err = closer.register(httpURLConnection.getErrorStream());
			copy(in, nullOutputStream());
			if (err != null) copy(err, nullOutputStream());
		} catch (Throwable t) {
			throw closer.rethrow(t);
		} finally {
			closer.close();
		}
	}

	private void writeResults(Server server, Query query, Iterable<Result> results, HttpURLConnection httpURLConnection) throws IOException {
		try (OutputStreamWriter outputStream = new OutputStreamWriter(httpURLConnection.getOutputStream(), charset)) {
			target.write(outputStream, server, query, results);
		}
	}

	private HttpURLConnection createHttpURLConnection() throws IOException {
		if (proxy == null) return (HttpURLConnection) url.openConnection();
		else return  (HttpURLConnection) url.openConnection(proxy);
	}

}
