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
package com.googlecode.jmxtrans.cli;

import com.beust.jcommander.IDefaultProvider;

import javax.annotation.Nonnull;
import java.util.Properties;

public class FileConfiguration implements IDefaultProvider {

	@Nonnull private final Properties properties;

	public FileConfiguration(@Nonnull Properties properties) {
		this.properties = properties;
	}

	@Override
	public String getDefaultValueFor(String optionName) {
		switch (optionName) {
			case "-c":
			case "--continue-on-error":
				return properties.getProperty("continue.on.error");
			case "-j":
			case "--json-directory":
				return properties.getProperty("json.directory");
			case "-f":
			case "--json-file":
				return properties.getProperty("json.file");
			case "--config":
				return properties.getProperty("config.file");
			case "-e":
			case "--run-endlessly":
				return properties.getProperty("run.endlessly");
			case "-q":
			case "--quartz-properties-file":
				return properties.getProperty("quartz.properties.file");
			case "-s":
			case "--run-period-in-seconds":
				return properties.getProperty("run.period.in.seconds");
			case "-a":
			case "--additional-jars":
				return properties.getProperty("additional.jars");
			case "--query-processor-executor-pool-size":
				return properties.getProperty("query.processor.executor.pool.size");
			case "--query-processor-executor-work-queue-capacity":
				return properties.getProperty("query.processor.executor.work.queue.capacity");
			case "--result-processor-executor-pool-size":
				return properties.getProperty("result.processor.executor.pool.size");
			case "--result-processor-executor-work-queue-capacity":
				return properties.getProperty("result.processor.executor.work.queue.capacity");
			default:
				return null;
		}
	}
}
