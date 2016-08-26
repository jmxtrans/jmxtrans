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

import org.junit.Test;

import static com.googlecode.jmxtrans.model.output.support.pool.FlushStrategyUtils.createFlushStrategy;
import static org.assertj.core.api.Assertions.assertThat;

public class FlushStrategyUtilsTest {

	@Test
	public void defaultStrategyIsNeverFlush() {
		FlushStrategy strategy = createFlushStrategy(null, null);
		assertThat(strategy).isInstanceOf(NeverFlush.class);
	}

	@Test
	public void createNeverFlush() {
		FlushStrategy strategy = createFlushStrategy("never", null);
		assertThat(strategy).isInstanceOf(NeverFlush.class);
	}

	@Test
	public void createAlwaysFlush() {
		FlushStrategy strategy = createFlushStrategy("always", null);
		assertThat(strategy).isInstanceOf(AlwaysFlush.class);
	}

	@Test
	public void createTimeBasedFlush() {
		FlushStrategy strategy = createFlushStrategy("timeBased", 1);
		assertThat(strategy).isInstanceOf(TimeBasedFlush.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void flushDelayIsRequiredForTimeBasedFlush() {
		createFlushStrategy("timeBased", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unkownStrategyThrowsException() {
		createFlushStrategy("unkown", null);
	}
}
