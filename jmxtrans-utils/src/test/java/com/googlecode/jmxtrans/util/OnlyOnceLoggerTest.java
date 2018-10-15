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
package com.googlecode.jmxtrans.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OnlyOnceLoggerTest {

	@Mock private Logger logger;

	@Before
	public void setupLogger() {
		when(logger.isInfoEnabled()).thenReturn(true);
	}

	@Test
	public void firstMessageShouldBeLogged() {
		new OnlyOnceLogger(logger)
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).info("my message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void sameMessageShouldOnlyBeLoggedOnce() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).info("my message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void differentMessagesShouldAllBeLogged() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my other message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).info("my message", "argument 1", "argument 2", "argument 3");
		verify(logger, times(1)).info("my other message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void differentArgumentsShouldAllBeLogged() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "other argument 2", "argument 3");

		verify(logger, times(1)).info("my message", "argument 1", "argument 2", "argument 3");
		verify(logger, times(1)).info("my message", "argument 1", "other argument 2", "argument 3");
	}

	@Test
	public void interleavedMessagesShouldBeLoggedOnlyOnce() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my other message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.infoOnce("my other message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).info("my message", "argument 1", "argument 2", "argument 3");
		verify(logger, times(1)).info("my other message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void nothingIsLoggedWhenMaxHistorySizeIsReached() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger, 50);
		for (int i = 0; i < 100; i++) {
			onlyOnceLogger
					.infoOnce("my message", "argument 1", "argument 2", i);
		}

		verify(logger, times(50)).info(anyString(), any(), any(), any());
	}

	@Test
	public void dontLogIfLoggingIsDisabled() {
		reset(logger);
		when(logger.isInfoEnabled()).thenReturn(false);

		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.infoOnce("my message", "argument 1", "argument 2", "argument 3");

		verify(logger, never()).info(anyString(), any(), any(), any());
	}

}
