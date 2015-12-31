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

import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import static com.google.common.base.Preconditions.checkArgument;

public class HttpUrlConnectionConfigurer {
	@Nonnull private final String requestMethod;
	private final int readTimeoutInMillis;
	@Nullable private final String authorization;
	@Nonnull private final String userAgent;
	@Nullable private final String contentType;

	public HttpUrlConnectionConfigurer(
			@Nonnull String requestMethod,
			int readTimeoutInMillis,
			@Nullable String authorization,
			@Nullable String contentType) {
		checkArgument(methodIsValid(requestMethod), "%s is not a supported HTTP method", requestMethod);
		this.requestMethod = requestMethod;
		this.readTimeoutInMillis = readTimeoutInMillis;
		this.authorization = authorization;
		this.userAgent = "jmxtrans-standalone/1 " + "(" +
				System.getProperty("java.vm.name") + "/" + System.getProperty("java.version") + "; " +
				System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "/" + System.getProperty("os.version")
				+ ")";
		this.contentType = contentType;
	}

	public void configure(HttpURLConnection httpURLConnection) throws ProtocolException {
		httpURLConnection.setRequestMethod(requestMethod);
		httpURLConnection.setDoInput(true);
		httpURLConnection.setDoOutput(true);
		httpURLConnection.setReadTimeout(readTimeoutInMillis);
		if (contentType != null) httpURLConnection.setRequestProperty("Content-Type", contentType);
		if (authorization != null) httpURLConnection.setRequestProperty("Authorization", authorization);
		httpURLConnection.setRequestProperty("User-Agent", userAgent);
	}

	private static boolean methodIsValid(String requestMethod) {
		return requestMethod != null
				&& requestMethod.equals("POST");
	}

	public static Builder builder(String requestMethod) {
		return new Builder(requestMethod);
	}

	@Accessors(chain = true)
	public static final class Builder {
		@Nonnull private final String requestMethod;
		@Setter private int readTimeoutInMillis = 100;
		@Setter @Nullable private String authorization = null;
		@Setter @Nullable private String contentType = null;

		public Builder(@Nonnull String requestMethod) {
			this.requestMethod = requestMethod;
		}

		public HttpUrlConnectionConfigurer build() {
			return new HttpUrlConnectionConfigurer(
					requestMethod,
					readTimeoutInMillis,
					authorization,
					contentType
			);
		}
	}
}
