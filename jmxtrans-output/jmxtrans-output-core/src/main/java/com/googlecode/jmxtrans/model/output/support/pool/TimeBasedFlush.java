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
package com.googlecode.jmxtrans.model.output.support.pool;

import com.googlecode.jmxtrans.util.Clock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Flushable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@ThreadSafe
public class TimeBasedFlush implements FlushStrategy {

	@Nonnull private long lastFlush;
	@Nonnull private final Clock clock;
	@Nonnull private final long flushPeriodMillisecond;

	public TimeBasedFlush(@Nonnull Clock clock, long flushPeriod, @Nonnull TimeUnit unit) {
		this.clock = clock;
		this.lastFlush = clock.currentTimeMillis();
		this.flushPeriodMillisecond = MILLISECONDS.convert(flushPeriod, unit);
	}


	@Override
	public void flush(@Nonnull Flushable flushable) throws IOException {
		if (shouldFlush()) flushable.flush();
	}

	private synchronized boolean shouldFlush() {
		if (lastFlush + flushPeriodMillisecond < clock.currentTimeMillis()) {
			lastFlush = clock.currentTimeMillis();
			return true;
		}
		return false;
	}
}
