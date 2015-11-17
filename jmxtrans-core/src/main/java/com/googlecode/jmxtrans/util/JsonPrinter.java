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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.googlecode.jmxtrans.model.JmxProcess;

import java.io.PrintStream;

public class JsonPrinter {

	private final PrintStream out;
	private final ObjectMapper mapper;
	private final ObjectWriter prettyPrintingWriter;

	public JsonPrinter(PrintStream out) {
		this.out = out;
		mapper = new ObjectMapper();
		mapper.getSerializationConfig().without(SerializationFeature.WRITE_NULL_MAP_VALUES);
		//mapper.getSerializationConfig().set(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		prettyPrintingWriter = mapper.writerWithDefaultPrettyPrinter();
	}

	/**
	 * Utility function good for testing things. Prints out the json tree of the
	 * JmxProcess.
	 */
	public void print(JmxProcess process) throws Exception {
		mapper.writeValue(out, process);
	}

	/**
	 * Utility function good for testing things. Prints out the json tree of the
	 * JmxProcess.
	 */
	public void prettyPrint(JmxProcess process) throws Exception {
		prettyPrintingWriter.writeValue(out, process);
	}
}
