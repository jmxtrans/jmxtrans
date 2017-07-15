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
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import org.apache.commons.lang.StringUtils;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GelfWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(GelfWriter.class);

	private final Map<String, Object> additionalFields;
	private final GelfConfiguration gelfConfiguration;

	private boolean useMock = false;

	public GelfWriter(
		@JsonProperty("typeNames") final ImmutableList<String> typeNames,
		@JsonProperty("booleanAsNumber") final boolean booleanAsNumber,
		@JsonProperty("debugEnabled") final Boolean debugEnabled,
		@JsonProperty("settings") final Map<String, Object> settings,
		@JsonProperty("host") final String host,
		@JsonProperty("port") Integer port,
		@JsonProperty("additionalFields") final Map<String, Object> additionalFields,
		@JsonProperty("transport") final String transport,
		@JsonProperty("queueSize") final Integer queueSize,
		@JsonProperty("connectTimeout") final Integer connectTimeout,
		@JsonProperty("reconnectDelay") final Integer reconnectDelay,
		@JsonProperty("tcpNoDelay") final Boolean tcpNoDelay,
		@JsonProperty("sendBufferSize") final Integer sendBufferSize,
		@JsonProperty("tlsEnabled") final Boolean tlsEnabled,
		@JsonProperty("tlsTrustCertChainFile") final String tlsTrustCertChainFile,
		@JsonProperty("tlsCertVerificationEnabled") final Boolean tlsCertVerificationEnabled,
		@JsonProperty("tcpKeepAlive") final Boolean tcpKeepAlive,
		@JsonProperty("maxInflightSends") final Integer maxInflightSends
	) {
		super(typeNames, booleanAsNumber, debugEnabled, settings);
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
				case "TCP": gelfTransports = GelfTransports.TCP;
					break;
				case "UDP": gelfTransports = GelfTransports.UDP;
					break;
				case "TEST": useMock = true;
				default: gelfTransports = GelfTransports.TCP;
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
			gelfConfiguration.tlsTrustCertChainFile(new File(tlsTrustCertChainFile));
		}

		if (maxInflightSends != null) {
			gelfConfiguration.maxInflightSends(maxInflightSends);
		}

		log.debug("Created gelf configuration: %s", gelfConfiguration.toString());

	}

	@Override
	public void validateSetup(final Server server, final Query query)
		throws ValidationException
	{
		// no validations
	}

	@Override
	protected void internalWrite(
		final Server server,
		final Query query,
		final ImmutableList<Result> results) throws Exception
	{
		final List<String> typeNames = this.getTypeNames();

		final GelfTransport transport;

		if (useMock) {
			transport = new MockGelfTransport();
		} else {
			transport = GelfTransports.create(this.gelfConfiguration);
		}

		final GelfMessageBuilder messageBuilder = new GelfMessageBuilder(
			"",
			server.getHost()
		);

		final List<String> messages = new ArrayList<>();

		for (final Result result : results) {
			log.debug("Query result: [{}]", result);
			for (final Map.Entry<String, Object> values: result.getValues().entrySet()) {
				final String key = KeyUtils.getKeyString(query, result, values, typeNames);

				messages.add(
					String.format(
						"%s=%s",
						key,
						values.getValue()
					)
				);

				messageBuilder.additionalField(key, values.getValue());
			}
		}

		if (additionalFields != null) {
			for (final Map.Entry<String, Object> additionalField : additionalFields
				.entrySet()) {
				messageBuilder.additionalField(additionalField.getKey(),
					additionalField.getValue());
			}
		}

		final String message = StringUtils.join(messages, " ");

		messageBuilder.message(message);
		messageBuilder.fullMessage(message);

		log.debug(
			String.format(
				"Sending GELF message: %s",
				messageBuilder.build().toString()
			)
		);

		transport.send(messageBuilder.build());
	}
}
