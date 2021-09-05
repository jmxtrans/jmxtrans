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

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.JmxErrorHandlingEnum;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

@ThreadSafe
@ToString(exclude = {"resultProcessor"})
public class ProcessQueryThread implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Nonnull private final Server server;
	@Nonnull private final Query query;
	@Nonnull private final ResultProcessor resultProcessor;

	public ProcessQueryThread(@Nonnull ResultProcessor resultProcessor, @Nonnull Server server, @Nonnull Query query) {
		this.resultProcessor = resultProcessor;
		this.server = server;
		this.query = query;
	}

	@Override
	public void run() {
		try {
			Collection<Result> results = null;
			try {
				results = server.execute(query);
			} catch (Exception e) {
				if (query.getJmxErrorHandling() == JmxErrorHandlingEnum.DUMP) {
					throw e;
				} else if (query.getJmxErrorHandling() == JmxErrorHandlingEnum.WARN) {
					log.warn("Error executing query {} on server {}", query, server);
				} else if (query.getJmxErrorHandling() == JmxErrorHandlingEnum.IGNORE) {
					// Ignore any message
				}
			}
			if (results != null) {
				resultProcessor.submit(server, query, results);
			}
		} catch (Exception e) {
			log.error("Error executing query {} on server {}", query, server, e);
			throw new RuntimeException(e);
		}
	}
}
