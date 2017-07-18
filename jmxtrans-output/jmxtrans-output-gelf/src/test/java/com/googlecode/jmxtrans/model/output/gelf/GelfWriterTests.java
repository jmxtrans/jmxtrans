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
import com.googlecode.jmxtrans.model.ResultFixtures;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GelfWriterTests {

	@Captor
	private
	ArgumentCaptor<GelfMessage> messageArgumentCaptor;

	@Mock
	private
	GelfTransport gelfTransport;

	@Test
	public void testSendGelfMessage() throws Exception {
		final ImmutableList<String> typenames = ImmutableList.of();
		final Map<String, Object> settings = new HashMap<>();

		doNothing().when(gelfTransport).send(
			messageArgumentCaptor.capture()
		);

		final GelfWriter gelfWriter;

		gelfWriter = new GelfWriter(
			typenames,
			ImmutableMap.of("type", (Object) "jmx"),
			gelfTransport
		);
		gelfWriter.start();
		gelfWriter.doWrite(dummyServer(),
			dummyQuery(),
			ResultFixtures.singleNumericResult());

		final GelfMessage gelfMessage = messageArgumentCaptor.getValue();

		assertThat(gelfMessage.getAdditionalFields())
			.containsEntry("type", "jmx")
			.as("Additional fields were not checked");

		assertThat(gelfMessage.getFullMessage())
			.isNotEmpty()
			.as("Invalid full message");

		assertThat(gelfMessage.getMessage())
			.isNotEmpty()
			.as("Invalid message");

		verify(
			gelfTransport,
			times(1)
		).send(isA(GelfMessage.class));
	}

}
