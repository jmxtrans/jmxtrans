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
package com.googlecode.jmxtrans.cli;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;

public class JmxTransConfiguration {
	/**
	 * If it is false, then JmxTrans will stop when one of the JSON
	 * configuration file is invalid. Otherwise, it will just print an error
	 * and continue processing.
	 */
	@Getter @Setter
	private boolean continueOnJsonError = false;

	@Getter @Setter
	private File jsonDirOrFile;

	/**
	 * If this is true, then this class will execute the main() loop and then
	 * wait 60 seconds until running again.
	 */
	@Getter @Setter
	private boolean runEndlessly = false;
	/**
	 * The Quartz server properties.
	 */
	@Getter @Setter
	private String quartzPropertiesFile = null;

	/**
	 * The seconds between server job runs.
	 */
	@Getter @Setter
	private int runPeriod = 60;

	@Getter @Setter
	private boolean help = false;

	@Getter @Setter
	private List<File> additionalJars = ImmutableList.of();

}
