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
package com.googlecode.jmxtrans.model;

import com.googlecode.jmxtrans.test.SlowTest;
import net.jodah.failsafe.CircuitBreakerOpenException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.dummyResults;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerOutputWriterTest {

	@Mock private OutputWriter target;

	@Test
	public void writesArePassedToTheTargetOutputWriter() throws Exception {
		CircuitBreakerOutputWriter<OutputWriter> circuitBreaker = new CircuitBreakerOutputWriter<>(target);
		circuitBreaker.doWrite(dummyServer(), dummyQuery(), dummyResults());

		verify(target).doWrite(dummyServer(), dummyQuery(), dummyResults());
	}

	@Test
	public void circuitBreakerOpensAfterThreeFailures() throws Exception {
		CircuitBreakerOutputWriter<OutputWriter> circuitBreaker = new CircuitBreakerOutputWriter<>(target);

		doThrow(new IllegalStateException("failing output writer")).when(target).doWrite(dummyServer(), dummyQuery(), dummyResults());

		for (int i = 0; i < 20; i++) {
			try {
				circuitBreaker.doWrite(dummyServer(), dummyQuery(), dummyResults());
				fail("Exception should have been thrown");
			} catch (IllegalStateException ignore) {
				assertThat(ignore).hasMessage("failing output writer");
			} catch (CircuitBreakerOpenException ignore) {}
		}

		verify(target, times(10)).doWrite(dummyServer(), dummyQuery(), dummyResults());
	}

	@Test
	@Category(SlowTest.class)
	@Ignore("Long running test, don't run it each time.")
	public void circuitBreakerOpensOnTimeouts() throws Exception {
		CircuitBreakerOutputWriter<OutputWriter> circuitBreaker = new CircuitBreakerOutputWriter<>(target);

		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(260);
				return null;
			}
		}).when(target).doWrite(dummyServer(), dummyQuery(), dummyResults());

		for (int i = 0; i < 20; i++) {
			try {
				circuitBreaker.doWrite(dummyServer(), dummyQuery(), dummyResults());
			} catch (Exception ignore) {
			}
		}

		verify(target, times(10)).doWrite(dummyServer(), dummyQuery(), dummyResults());
	}
}
