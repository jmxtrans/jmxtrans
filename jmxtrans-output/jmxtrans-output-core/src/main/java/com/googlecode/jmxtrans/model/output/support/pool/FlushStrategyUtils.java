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

import com.googlecode.jmxtrans.util.SystemClock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class FlushStrategyUtils {
	private FlushStrategyUtils() {}

	@Nonnull
	public static FlushStrategy createFlushStrategy(@Nullable String strategy, @Nullable Integer flushDelayInSeconds) {
		if (strategy == null) return new NeverFlush();
		if (strategy.equals("never")) return new NeverFlush();
		if (strategy.equals("always")) return new AlwaysFlush();
		if (strategy.equals("timeBased")) {
			if (flushDelayInSeconds == null) throw new IllegalArgumentException("flushDelayInSeconds cannot be null");
			return new TimeBasedFlush(new SystemClock(), flushDelayInSeconds, SECONDS);
		}
		throw new IllegalArgumentException(
				format("Strategy %s is not valid, supported values are 'never', 'always' and 'timeBased'", strategy));
	}

}
