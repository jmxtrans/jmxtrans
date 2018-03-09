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
package com.googlecode.jmxtrans.jmx;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.collect.Iterables.concat;

public class ResultProcessor {

	private final Logger logger = LoggerFactory.getLogger(ResultProcessor.class);

	@Nonnull private final ExecutorRepository resultExecutorRepository;

	@Inject
	public ResultProcessor(
			@Nonnull @Named("resultExecutorRepository") ExecutorRepository resultExecutorRepository
	) {
		this.resultExecutorRepository = resultExecutorRepository;
	}

	public void submit(@Nonnull final Server server, @Nonnull final Query query, @Nonnull final Iterable<Result> results) {
		final ThreadPoolExecutor executor = resultExecutorRepository.getExecutor(server);

		for (final OutputWriter writer : concat(query.getOutputWriterInstances(), server.getOutputWriters())) {
			try {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							writer.doWrite(server, query, results);
						} catch (Exception e) {
							logger.warn("Could not write results {} of query {} to output writer {}", results, query, writer, e);
						}
					}
				});
			} catch (RejectedExecutionException ree) {
				logger.error("Could not submit results {} of query {} to output writer {}. You could try to size the 'resultProcessorExecutor' to a larger size.", results, query, writer, ree);
			}
		}
	}
}
