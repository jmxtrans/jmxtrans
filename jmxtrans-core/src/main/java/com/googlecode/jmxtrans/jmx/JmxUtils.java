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
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The worker code.
 *
 * @author jon
 */
public class JmxUtils {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Does the work for processing a Server object.
	 */
	public void processServer(Server server) throws Exception {

		if (server.isQueriesMultiThreaded()) {
			ExecutorService service = null;
			try {
				service = Executors.newFixedThreadPool(server.getNumQueryThreads());
				if (log.isDebugEnabled()) {
					log.debug("----- Creating " + server.getQueries().size() + " query threads");
				}

				List<Callable<Object>> threads = new ArrayList<Callable<Object>>(server.getQueries().size());
				for (Query query : server.getQueries()) {
					ProcessQueryThread pqt = new ProcessQueryThread(server, query);
					threads.add(Executors.callable(pqt));
				}

				service.invokeAll(threads);

			} finally {
				if (service != null) {
					shutdownAndAwaitTermination(service);
				}
			}
		} else {
			for (Query query : server.getQueries()) {
				Iterable<Result> results = server.execute(query, new Timeout(1, SECONDS));
				query.runOutputWritersForQuery(server, results);
			}
		}
	}

	/**
	 * Copied from the Executors javadoc.
	 */
	private void shutdownAndAwaitTermination(ExecutorService service) {
		service.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
				service.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
					log.error("Pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			service.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
