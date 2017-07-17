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
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultFixtures;
import com.googlecode.jmxtrans.model.ValidationException;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTcpTransport;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GelfWriterTests {

	private Result result;

	@Before
	public void setUp() throws Exception {
		this.result = ResultFixtures.numericResult();
	}

	@Test
	public void testSendGelfMessage() throws Exception {
		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfTransport gelfTransport = Mockito.mock(GelfTcpTransport.class);

		doNothing().when(gelfTransport).send(
			argThat(
				new ArgumentMatcher<GelfMessage>() {
					@Override
					public boolean matches(final Object o) {
						final GelfMessage message = (GelfMessage) o;
						assertThat(message.getAdditionalFields())
							.containsKey("test")
							.as("Additional fields were not checked");

						assertThat(message.getFullMessage())
							.isNotEmpty()
							.as("Invalid full message");
						assertThat(message.getMessage())
							.isNotEmpty()
							.as("Invalid message");
						return true;
					}
				}
			)
		);

		final GelfWriter gelfWriter;

		gelfWriter = new GelfWriter(
			typenames,
			true,
			true,
			settings,
			additionalFields,
			gelfTransport
		);
		gelfWriter.start();
		gelfWriter.doWrite(dummyServer(),
			dummyQuery(),
			ImmutableList.of(result));

		verify(
			gelfTransport,
			times(1)
		).send(isA(GelfMessage.class));
	}

	@Test
	public void testTcpDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			additionalFields,
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

		final GelfWriter gelfWriter = gelfWriterFactory.create();
		gelfWriter.validateSetup(dummyServer(), dummyQuery());

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testUdpDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			additionalFields,
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

		final GelfWriter gelfWriter = gelfWriterFactory.create();
		gelfWriter.validateSetup(dummyServer(), dummyQuery());

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testNonDefaultsWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			12202,
			additionalFields,
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

		final GelfWriter gelfWriter = gelfWriterFactory.create();
		gelfWriter.validateSetup(dummyServer(), dummyQuery());

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testNonDefaultsFalseWriterFactory() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			additionalFields,
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

		final GelfWriter gelfWriter = gelfWriterFactory.create();
		gelfWriter.validateSetup(dummyServer(), dummyQuery());

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}
	@Test
	public void testIgnoreWrongTransport() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
			typenames,
			true,
			true,
			settings,
			"test",
			null,
			additionalFields,
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
		gelfWriter.validateSetup(dummyServer(), dummyQuery());

		assertThat(gelfWriter).isNotNull().as("Didn't get a writer back.");
	}

	@Test
	public void testHostNotSpecified() throws ValidationException {

		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		final Map<String, Object> additionalFields = new HashMap<>();

		additionalFields.put("test", "test");

		try {
			final GelfWriterFactory gelfWriterFactory = new GelfWriterFactory(
				typenames,
				true,
				true,
				settings,
				null,
				null,
				additionalFields,
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
