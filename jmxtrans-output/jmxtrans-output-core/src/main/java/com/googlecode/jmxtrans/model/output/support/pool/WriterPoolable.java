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

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Poolable;
import stormpot.Slot;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;

public class WriterPoolable implements Poolable {

	private static final Logger logger = LoggerFactory.getLogger(WriterPoolable.class);

	@Nonnull private final Slot slot;

	@Nonnull @Getter private final Writer writer;

	@Nonnull private final FlushStrategy flushStrategy;

	public WriterPoolable(@Nonnull Slot slot, @Nonnull Writer writer, @Nonnull FlushStrategy flushStrategy) {
		this.slot = slot;
		this.writer = writer;
		this.flushStrategy = flushStrategy;
	}

	@Override
	public void release() {
		try {
			flushStrategy.flush(writer);
			slot.release(this);
		} catch (IOException ioe) {
			logger.error("Could not flush writer", ioe);
			invalidate();
		}
	}

	public void invalidate() {
		slot.expire(this);
	}
}
