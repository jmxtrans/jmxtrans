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

import static java.util.concurrent.TimeUnit.SECONDS;

public class ProcessQueryThread implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Server server;
	private final Query query;

	public ProcessQueryThread(Server server, Query query) {
		this.server = server;
		this.query = query;
	}

	public void run() {
		try {
			Iterable<Result> results = server.execute(query, new Timeout(1, SECONDS));
			query.runOutputWritersForQuery(server, results);
			server.runOutputWriters(query, results);
		} catch (Exception e) {
			log.error("Error executing query: " + query, e);
			throw new RuntimeException(e);
		}
	}
}
