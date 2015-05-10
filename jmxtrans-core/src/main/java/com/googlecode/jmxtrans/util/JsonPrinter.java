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
