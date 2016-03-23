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
package com.googlecode.jmxtrans.jmx;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The worker code.
 *
 * @author jon
 */
public class JmxUtils {

	@Nonnull private static final Logger logger = LoggerFactory.getLogger(JmxUtils.class);

	@Nonnull private final ThreadPoolExecutor executorService;
	@Nonnull private final ResultProcessor resultProcessor;

	@Inject
	public JmxUtils(
			@Named("queryProcessorExecutor") @Nonnull ThreadPoolExecutor executorService,
			@Nonnull ResultProcessor resultProcessor) {
		this.executorService = executorService;
		this.resultProcessor = resultProcessor;
	}

	public void processServer(Server server) throws Exception {
		for (Query query : server.getQueries()) {
			ProcessQueryThread pqt = new ProcessQueryThread(resultProcessor, server, query);
			try {
				executorService.submit(pqt);
			} catch (RejectedExecutionException ree) {
				logger.error("Could not submit query {}. You could try to size the 'queryProcessorExecutor' to a larger size.", pqt, ree);
			}
		}
	}
}
