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
package com.googlecode.jmxtrans.model.output.gelf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import lombok.EqualsAndHashCode;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Map;

@EqualsAndHashCode
public class GelfWriterFactory implements OutputWriterFactory {

	private final ImmutableList<String> typeNames;
	private final GelfConfiguration gelfConfiguration;
	private final Map<String, Object> additionalFields;
	private static final Logger log = LoggerFactory.getLogger(GelfWriter.class);

	public GelfWriterFactory(
		@JsonProperty("typeNames") final ImmutableList<String> typeNames,
		@JsonProperty("host") final String host,
		@JsonProperty("port") final Integer port,
		@JsonProperty("additionalFields")
		final Map<String, Object> additionalFields,
		@JsonProperty("transport") final String transport,
		@JsonProperty("queueSize") final Integer queueSize,
		@JsonProperty("connectTimeout") final Integer connectTimeout,
		@JsonProperty("reconnectDelay") final Integer reconnectDelay,
		@JsonProperty("tcpNoDelay") final Boolean tcpNoDelay,
		@JsonProperty("sendBufferSize") final Integer sendBufferSize,
		@JsonProperty("tlsEnabled") final Boolean tlsEnabled,
		@JsonProperty("tlsTrustCertChainFile")
		final String tlsTrustCertChainFile,
		@JsonProperty("tlsCertVerificationEnabled")
		final Boolean tlsCertVerificationEnabled,
		@JsonProperty("tcpKeepAlive") final Boolean tcpKeepAlive,
		@JsonProperty("maxInflightSends") final Integer maxInflightSends
	)
	{
		this.typeNames = typeNames;

		this.additionalFields = additionalFields;

		if (host == null) {
			throw new NullPointerException("Host can not be null");
		}

		if (port != null) {
			this.gelfConfiguration = new GelfConfiguration(host, port);
		} else {
			this.gelfConfiguration = new GelfConfiguration(host);
		}

		if (transport != null) {
			final GelfTransports gelfTransports;

			switch (transport.toUpperCase()) {
				case "TCP":
					gelfTransports = GelfTransports.TCP;
					break;
				case "UDP":
					gelfTransports = GelfTransports.UDP;
					break;
				default:
					gelfTransports = GelfTransports.TCP;
			}

			gelfConfiguration.transport(gelfTransports);
		}

		if (queueSize != null) {
			gelfConfiguration.queueSize(queueSize);
		}

		if (connectTimeout != null) {
			gelfConfiguration.connectTimeout(connectTimeout);
		}

		if (reconnectDelay != null) {
			gelfConfiguration.reconnectDelay(reconnectDelay);
		}

		if (tcpNoDelay != null) {
			gelfConfiguration.tcpNoDelay(tcpNoDelay);
		}

		if (sendBufferSize != null) {
			gelfConfiguration.sendBufferSize(sendBufferSize);
		}

		if (tlsEnabled != null) {
			if (tlsEnabled) {
				gelfConfiguration.enableTls();
			} else {
				gelfConfiguration.disableTls();
			}
		}

		if (tlsCertVerificationEnabled != null) {
			if (tlsCertVerificationEnabled) {
				gelfConfiguration.enableTlsCertVerification();
			} else {
				gelfConfiguration.disableTlsCertVerification();
			}
		}

		if (tlsTrustCertChainFile != null) {
			gelfConfiguration.tlsTrustCertChainFile(new File(
				tlsTrustCertChainFile));
		}

		if (tcpKeepAlive != null) {
			gelfConfiguration.tcpKeepAlive(tcpKeepAlive);
		}

		if (maxInflightSends != null) {
			gelfConfiguration.maxInflightSends(maxInflightSends);
		}

		log.debug("Created gelf configuration: {}",
			gelfConfiguration.toString());

	}

	@Nonnull
	@Override
	public GelfWriter create() {
		final GelfTransport gelfTransport = GelfTransports.create(
			gelfConfiguration
		);

		return new GelfWriter(
			this.typeNames,
			this.additionalFields,
			gelfTransport
		);
	}

	@VisibleForTesting
	public GelfConfiguration getGelfConfiguration() {
		return gelfConfiguration;
	}
}
