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
package com.googlecode.jmxtrans.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OnlyOnceLoggerTest {

	@Mock private Logger logger;

	@Test
	public void firstMessageShouldBeLogged() {
		new OnlyOnceLogger(logger)
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).warn("my message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void sameMessageShouldOnlyBeLoggedOnce() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).warn("my message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void differentMessagesShouldAllBeLogged() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my other message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).warn("my message", "argument 1", "argument 2", "argument 3");
		verify(logger, times(1)).warn("my other message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void differentArgumentsShouldAllBeLogged() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "other argument 2", "argument 3");

		verify(logger, times(1)).warn("my message", "argument 1", "argument 2", "argument 3");
		verify(logger, times(1)).warn("my message", "argument 1", "other argument 2", "argument 3");
	}

	@Test
	public void interleavedMessagesShouldBeLoggedOnlyOnce() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger);
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my other message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my message", "argument 1", "argument 2", "argument 3");
		onlyOnceLogger
				.warnOnce("my other message", "argument 1", "argument 2", "argument 3");

		verify(logger, times(1)).warn("my message", "argument 1", "argument 2", "argument 3");
		verify(logger, times(1)).warn("my other message", "argument 1", "argument 2", "argument 3");
	}

	@Test
	public void nothingIsLoggedWhenMaxHistorySizeIsReached() {
		OnlyOnceLogger onlyOnceLogger = new OnlyOnceLogger(logger, 50);
		for (int i = 0; i < 100; i++) {
			onlyOnceLogger
					.warnOnce("my message", "argument 1", "argument 2", i);
		}

		verify(logger, times(50)).warn(anyString(), any(), any(), any());
	}

}
