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
import lombok.ToString;

import javax.annotation.Nonnull;

@ToString
@EqualsAndHashCode(of = "outputWriterFactory")
public class SingletonOutputWriterFactory<T extends OutputWriter> implements OutputWriterFactory<T> {

	@Nonnull private final T outputWriter;

	@Nonnull private final OutputWriterFactory<T> outputWriterFactory;

	public SingletonOutputWriterFactory(@Nonnull OutputWriterFactory<T> outputWriterFactory) {
		this.outputWriterFactory = outputWriterFactory;
		outputWriter = outputWriterFactory.create();
	}

	@Override
	public T create() {
		return outputWriter;
	}
}
