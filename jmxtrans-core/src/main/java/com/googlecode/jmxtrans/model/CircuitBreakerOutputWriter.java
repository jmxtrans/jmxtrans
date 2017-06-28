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

import com.googlecode.jmxtrans.exceptions.LifecycleException;
import lombok.EqualsAndHashCode;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.function.CheckedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static net.jodah.failsafe.Failsafe.with;

@EqualsAndHashCode(exclude = "breaker")
public class CircuitBreakerOutputWriter<T extends OutputWriter> extends OutputWriterAdapter {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerOutputWriter.class);
	private final T target;
	private final CircuitBreaker breaker;

	public CircuitBreakerOutputWriter(final T target) {
		this.target = target;
		this.breaker = new CircuitBreaker()
				.withFailureThreshold(3, 10)
				.withSuccessThreshold(1)
				.withDelay(1, MINUTES)
				.withTimeout(250, MILLISECONDS)
				.onOpen(createLogger("Circuit breaker opened for output writer {}",false))
				.onClose(createLogger("Circuit breaker closed for output writer {}", true))
				.onHalfOpen(createLogger("Circuit breaker half opened for output writer {}", true));
	}

	private CheckedRunnable createLogger(final String message, final boolean isSuccess) {
		return new CheckedRunnable() {
			@Override
			public void run() throws Exception {
				if (isSuccess) logger.warn(message, target);
				else logger.info(message, target);
			}
		};
	}


	@Override
	public void close() throws LifecycleException {
		target.close();
	}

	@Override
	public void doWrite(final Server server, final Query query, final Iterable<Result> results) throws Exception {
		with(breaker).run(new CheckedRunnable() {
			@Override
			public void run() throws Exception {
				target.doWrite(server, query, results);
			}
		});
	}

	@Override
	public String toString() {
		return target.toString();
	}
}
