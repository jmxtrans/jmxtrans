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

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.Charset;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;

public class SocketOutputWriter<T extends OutputStreamBasedOutputWriter> extends OutputWriterAdapter {

	@Nonnull private final Logger logger = LoggerFactory.getLogger(SocketOutputWriter.class);

	@Nonnull private final T target;
	@Nonnull private String host;
	@Nonnull private Integer port;
	private int socketTimeoutMillis;
	@Nonnull private final Charset charset;

	public SocketOutputWriter(
			@Nonnull T target,
			@Nonnull String host,
			@Nonnull Integer port,
			int socketTimeoutMillis,
			@Nullable Charset charset) {
		this.target = target;
		this.host = host;
		this.port = port;
		this.socketTimeoutMillis = socketTimeoutMillis;
		this.charset = charset == null ? UTF_8 : charset;
	}

	@Override
	public void doWrite(Server server, Query query, Iterable<Result> results) throws IOException {
		SocketAddress serverAddress = new InetSocketAddress(host, port);
		Socket socket = new Socket();
		socket.setKeepAlive(false);
		socket.connect(serverAddress, socketTimeoutMillis);
		target.write(socket.getOutputStream(), socket.getInputStream(), charset, server, query, results);
		socket.close();
	}

}
