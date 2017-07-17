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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.ValidationException;
import org.assertj.core.api.Condition;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfTransports;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class GelfWriterFactoryTests {
	@Test
	public void testTcpDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			ImmutableMap.of("test", (Object) "test"),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getHostname()
		)
			.isEqualTo("test")
			.as("Hostname is wrong");

		// Check, that default values were used.

		assertThat(gelfWriterFactory.getGelfConfiguration()).is(
			new GelfDefaultConfigurationCondition()
		).as("Values are not the expected default values");

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter)
			.isNotNull()
			.as("Didn't get a writer back.");
		assertThat(gelfWriter.getAdditionalFields())
			.containsEntry("test", "test")
			.as("Wrong additional fields set");
	}

	@Test
	public void testUdpDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			ImmutableMap.of("test", (Object) "test"),
			"udp",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getTransport()
		)
			.isEqualTo(GelfTransports.UDP)
			.as("Wrong transport selected");

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");

	}

	@Test
	public void testNonDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			12202,
			ImmutableMap.of("test", (Object) "test"),
			"udp",
			10,
			10,
			10,
			true,
			10,
			true,
			"/somefile",
			true,
			true,
			10
		);

		assertThat(
			gelfWriterFactory.getGelfConfiguration()
		).is(
			new Condition<GelfConfiguration>() {
				@Override
				public boolean matches(final GelfConfiguration gelfConfiguration) {

					if (gelfConfiguration.getQueueSize() != 10) {
						return false;
					}

					if (gelfConfiguration.getConnectTimeout() != 10) {
						return false;
					}

					if (gelfConfiguration.getReconnectDelay() != 10) {
						return false;
					}

					if (!gelfConfiguration.isTcpNoDelay()) {
						return false;
					}

					if (gelfConfiguration.getSendBufferSize() != 10) {
						return false;
					}

					if (!gelfConfiguration.isTlsEnabled()) {
						return false;
					}

					if (!gelfConfiguration.getTlsTrustCertChainFile()
						.getPath()
						.equals("/somefile"))
					{
						return false;
					}

					if (!gelfConfiguration.isTlsCertVerificationEnabled()) {
						return false;
					}

					if (!gelfConfiguration.isTcpKeepAlive()) {
						return false;
					}

					if (gelfConfiguration.getMaxInflightSends() != 10) {
						return false;
					}

					return true;
				}
			}
		)
			.as("Custom configuration not used");

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testNonDefaultsFalseWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			ImmutableMap.of("test", (Object) "test"),
			"tcp",
			10,
			10,
			10,
			false,
			10,
			false,
			"/somefile",
			false,
			false,
			10
		);

		assertThat(gelfWriterFactory.getGelfConfiguration().isTcpNoDelay())
			.isFalse()
			.as("Custom configuration not used");

		assertThat(gelfWriterFactory.getGelfConfiguration().isTlsEnabled())
			.isFalse()
			.as("Custom configuration not used");

		assertThat(gelfWriterFactory.getGelfConfiguration()
			.isTlsCertVerificationEnabled())
			.isFalse()
			.as("Custom configuration not used");

		assertThat(gelfWriterFactory.getGelfConfiguration().isTcpKeepAlive())
			.isFalse()
			.as("Custom configuration not used");

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testIgnoreWrongTransport() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			ImmutableMap.of("test", (Object) "test"),
			"bogus",
			10,
			10,
			10,
			false,
			10,
			false,
			"/somefile",
			false,
			false,
			10
		);

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testHostNotSpecified() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		try {
			final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
				typenames,
				true,
				true,
				settings,
				null,
				null,
				ImmutableMap.of("test", (Object) "test"),
				"udp",
				10,
				10,
				10,
				true,
				10,
				true,
				"/somefile",
				true,
				true,
				10
			);
		} catch (final NullPointerException e) {
			assertThat(e).isNotNull();
			return;
		}

		fail("No error received");

	}
}
