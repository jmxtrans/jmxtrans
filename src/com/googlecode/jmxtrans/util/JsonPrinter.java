package com.googlecode.jmxtrans.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.PrintStream;

import com.googlecode.jmxtrans.model.JmxProcess;

import static org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_NULL_MAP_VALUES;

public class JsonPrinter {

	private final PrintStream out;
	private final ObjectMapper mapper;
	private final ObjectWriter prettyPrintingWriter;

	public JsonPrinter(PrintStream out) {
		this.out = out;
		mapper = new ObjectMapper();
		mapper.getSerializationConfig().set(WRITE_NULL_MAP_VALUES, false);
		prettyPrintingWriter = mapper.defaultPrettyPrintingWriter();
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
