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
package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ToString
public class FileWriter extends BaseOutputWriter {
	private static final String PROPERTY_BASEPATH = "filepath";
	private static final String PROPERTY_LINE_FORMAT = "lineFormat";
	private static final String DEFAULT_LINE_FORMAT = "%s=%s";
	private static final Logger log = LoggerFactory.getLogger(FileWriter.class);

	private File outputFile;
	private File outputTempFile;
	private String lineFormat;

	public FileWriter(@JsonProperty("typeNames") ImmutableList<String> typeNames,
					@JsonProperty("booleanAsNumber") boolean booleanAsNumber,
					@JsonProperty("debug") Boolean debugEnabled,
					@JsonProperty(PROPERTY_BASEPATH) String filepath,
					@JsonProperty(PROPERTY_LINE_FORMAT) String lineFormat,
					@JsonProperty("settings") Map<String, Object> settings) throws IOException {
		super(typeNames, booleanAsNumber, debugEnabled, settings);

		this.outputFile = new File(filepath);
		Path outputFilePath = outputFile.toPath();
		this.outputTempFile = new File(outputFilePath.getParent() + File.separator + "." + outputFilePath.getFileName());
		if (lineFormat == null) {
			this.lineFormat = DEFAULT_LINE_FORMAT;
		} else {
			this.lineFormat = lineFormat;
		}

		// make sure the permissions allow to manage these files:
		touch(this.outputFile);
		touch(this.outputTempFile);
	}

	private static void touch(File file) throws IOException {
		new FileOutputStream(file, true).close(); // ensure file exists and it's accessible

		assert(file.setLastModified(System.currentTimeMillis()));
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {}

	@Override
	protected void internalWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		try(PrintWriter outputTempPrintWriter = new PrintWriter(this.outputTempFile, "UTF-8")) {
			List<String> typeNames = this.getTypeNames();

			for (Result result : results) {
				log.debug(result.toString());
				outputTempPrintWriter.printf(lineFormat + System.lineSeparator(),
						KeyUtils.getKeyString(query, result, typeNames), result.getValue());
			}
		}
		assert(this.outputTempFile.renameTo(this.outputFile));
	}
}
