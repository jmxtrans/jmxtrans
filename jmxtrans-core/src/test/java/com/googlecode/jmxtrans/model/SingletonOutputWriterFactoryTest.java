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

import lombok.EqualsAndHashCode;
import org.junit.Test;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

public class SingletonOutputWriterFactoryTest {

	@Test
	public void sameInstanceIsAlwaysReturned() {
		SingletonOutputWriterFactory outputWriterFactory = new SingletonOutputWriterFactory(new DummyOutputWriterFactory());

		OutputWriter outputWriter1 = outputWriterFactory.create();
		OutputWriter outputWriter2 = outputWriterFactory.create();

		assertThat(outputWriter1).isSameAs(outputWriter2);
	}

	@Test
	public void twoSingletonFactoriesAreEqualIfTheyWrapTheSameFactory() {
		SingletonOutputWriterFactory outputWriterFactory1 = new SingletonOutputWriterFactory(new DummyOutputWriterFactory());
		SingletonOutputWriterFactory outputWriterFactory2 = new SingletonOutputWriterFactory(new DummyOutputWriterFactory());

		assertThat(outputWriterFactory1).isEqualTo(outputWriterFactory2);
	}

	@EqualsAndHashCode
	private static final class DummyOutputWriterFactory implements OutputWriterFactory {

		@Nonnull
		@Override
		public OutputWriter create() {
			return new OutputWriterAdapter() {
				@Override
				public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {

				}
			};
		}
	}

}
