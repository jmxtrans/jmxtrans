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
import org.graylog2.gelfclient.GelfTransports;
import org.junit.Test;

import java.io.File;
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
			"test",
			null,
			ImmutableMap.of("type", (Object) "jmx"),
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
		// Beware, as this may change with a new version of GelfClient

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getPort()
		).isEqualTo(12201)
			.as("Invalid port");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getTransport()
		).isEqualTo(GelfTransports.TCP)
			.as("Invalid transport");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getQueueSize()
		).isEqualTo(512)
			.as("Invalid queue size");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getConnectTimeout()
		).isEqualTo(1000)
			.as("Invalid connect timeout");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getReconnectDelay()
		).isEqualTo(500)
			.as("Invalid reconnect delay");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTcpNoDelay()
		).isFalse()
			.as("Invalid tcp nodelay");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getSendBufferSize()
		).isEqualTo(-1)
			.as("Invalid send buffer size");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTlsEnabled()
		).isFalse()
			.as("Invalid tls enabled");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTlsCertVerificationEnabled()
		).isTrue()
			.as("Invalid tls cert verification enabled");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTcpKeepAlive()
		).isFalse()
			.as("Invalid tcp keep alive");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getMaxInflightSends()
		).isEqualTo(512)
			.as("Invalid max inflight sends");

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter)
			.isNotNull()
			.as("Didn't get a writer back.");
		assertThat(gelfWriter.getAdditionalFields())
			.containsEntry("type", "jmx")
			.as("Wrong additional fields set");
	}

	@Test
	public void testUdpDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			"test",
			null,
			ImmutableMap.of("type", (Object) "jmx"),
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
			"test",
			12202,
			ImmutableMap.of("type", (Object) "jmx"),
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
			gelfWriterFactory.getGelfConfiguration().getQueueSize()
		).isEqualTo(10)
			.as("Invalid queue size");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getConnectTimeout()
		).isEqualTo(10)
			.as("Invalid connection timeout");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getReconnectDelay()
		).isEqualTo(10)
			.as("Invalid reconnect delay");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTcpNoDelay()
		).isTrue()
			.as("Invalid tcp no delay");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getSendBufferSize()
		).isEqualTo(10)
			.as("Invalid send buffer size");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getTlsTrustCertChainFile()
		).isInstanceOf(File.class)
			.as("Invalid tls trustcertchain file");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getTlsTrustCertChainFile()
		).hasName("somefile")
			.as("Invalid tls trustcertchain file");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTlsCertVerificationEnabled()
		).isTrue()
			.as("Invalid tls Cert verification enabled");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().isTcpKeepAlive()
		).isTrue()
			.as("Invalid tcp keep alive");

		assertThat(
			gelfWriterFactory.getGelfConfiguration().getMaxInflightSends()
		).isEqualTo(10)
			.as("Invalid max inflight sends");

		final GelfWriter gelfWriter = gelfWriterFactory.create();

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testNonDefaultsFalseWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			"test",
			null,
			ImmutableMap.of("type", (Object) "jmx"),
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
			"test",
			null,
			ImmutableMap.of("type", (Object) "jmx"),
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
				null,
				null,
				ImmutableMap.of("type", (Object) "jmx"),
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
