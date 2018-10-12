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
package com.googlecode.jmxtrans.scheduler;

import com.googlecode.jmxtrans.executors.ExecutorRepository;
import com.googlecode.jmxtrans.jmx.ProcessQueryThread;
import com.googlecode.jmxtrans.jmx.ResultProcessor;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Per server thread being run periodically
 */
public class ServerCommand implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ServerScheduler.class);
	private final Server server;
	private final ExecutorRepository queryExecutorRepository;
	private final ResultProcessor resultProcessor;

	public ServerCommand(Server server, ExecutorRepository queryExecutorRepository, ResultProcessor resultProcessor) {
		this.server = server;
		this.queryExecutorRepository = queryExecutorRepository;
		this.resultProcessor = resultProcessor;
	}

	public int getRunPeriodSeconds(int defaultRunPeriod) {
		return server.getRunPeriodSeconds() == null || server.getRunPeriodSeconds().intValue() <= 0 ?
				defaultRunPeriod :
				server.getRunPeriodSeconds().intValue();
	}

	@Override
	public void run() {

		log.debug("+++++ Started server job {}", server);
		try {
			final ThreadPoolExecutor executor = queryExecutorRepository.getExecutor(server);

			for (Query query : server.getQueries()) {
				ProcessQueryThread pqt = new ProcessQueryThread(resultProcessor, server, query);
				try {
					executor.submit(pqt);
				} catch (RejectedExecutionException ree) {
					log.error("Could not submit query {}. You could try to size the 'queryProcessorExecutor' to a larger size.", pqt, ree);
				}
			}
			log.debug("+++++ Finished server job {}", server);
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.warn("+++++ Failed server job " + server, e);
			} else {
				log.warn("+++++ Failed server job {}: {} {}", server, e.getClass().getName(), e.getMessage());
			}
		}

	}

	public String getName() {
		return server.getHost() + ":" + server.getPort();
	}
}
