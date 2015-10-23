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
package com.googlecode.jmxtrans.util;

import lombok.EqualsAndHashCode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * This class provides a simple filter on top of a logger to ensure that the same message is only logged once.
 *
 * Implementation relies on keeping the history of log messages in a HashMap.
 *
 * Limitations:
 *
 * <ul>
 *   <li>to limit the memory cost, history size is limited to 32K entries by default, once this limit is reached, no
 *   more messages are sent to the logger</li>
 *   <li>32K messages should be a reasonable limit, as long as message formats are similar they should all point to the
 *   same entry in the string cache</li>
 *   <li>log messages are compared using <code>hashCode()</code> and <code>equals()</code>, which means that those
 *   methods have better be fast on the arguments passed to the logger (probably not a problem) and that those methods
 *   have better provide a sensible meaning of <code>equals()</code> for our context (which is for example not the case
 *   for an array</li>
 * </ul>
 *
 * Note: if we ever need more than just a <code>warn()</code> method, we should implement this class as a
 * {@link java.lang.reflect.Proxy}.
 */
@ThreadSafe
public class OnlyOnceLogger {

	public static final int DEFAULT_HISTORY_SIZE = 32 * 1024;

	@Nonnull private final Logger logger;
	private final Set<LogEntry> alreadyLogged = new HashSet<LogEntry>();
	private final int maxHistorySize;

	public OnlyOnceLogger(Logger logger) {
		this(logger, DEFAULT_HISTORY_SIZE);
	}
	
	public OnlyOnceLogger(Logger logger, int maxHistorySize) {
		this.logger = logger;
		this.maxHistorySize = maxHistorySize;
	}

	public void warnOnce(String format, Object... arguments) {
		if (maxHistorySizeReached()) return;

		LogEntry logEntry = new LogEntry(format, arguments);
		if (shouldLog(logEntry)) logger.warn(format, arguments);
	}

	private synchronized boolean maxHistorySizeReached() {
		return alreadyLogged.size() >= maxHistorySize;
	}

	private synchronized boolean shouldLog(LogEntry logEntry) {
		if (alreadyLogged.contains(logEntry)) return false;
		alreadyLogged.add(logEntry);
		return true;
	}

	@EqualsAndHashCode
	private static final class LogEntry {
		private final String format;
		private final List<Object> arguments;

		private LogEntry(String format, Object[] arguments) {
			this.format = format;
			this.arguments = copyOf(arguments);
		}
	}

}
