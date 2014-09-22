package com.googlecode.jmxtrans.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.googlecode.jmxtrans.model.JmxProcess;

import static org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_NULL_MAP_VALUES;

public final class JsonUtils {

	private JsonUtils() {}

	/**
	 * Utility function good for testing things. Prints out the json tree of the
	 * JmxProcess.
	 */
	public static void printJson(JmxProcess process, PrintStream out) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.getSerializationConfig().set(WRITE_NULL_MAP_VALUES, false);
		out.println(mapper.writeValueAsString(process));
	}

	/**
	 * Utility function good for testing things. Prints out the json tree of the
	 * JmxProcess.
	 */
	public static void prettyPrintJson(JmxProcess process, PrintStream out) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.getSerializationConfig().set(WRITE_NULL_MAP_VALUES, false);
		ObjectWriter writer = mapper.defaultPrettyPrintingWriter();
		out.println(writer.writeValueAsString(process));
	}

	/**
	 * Uses jackson to load json configuration from a File into a full object
	 * tree representation of that json.
	 */
	public static JmxProcess getJmxProcess(File file) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JmxProcess jmx = mapper.readValue(file, JmxProcess.class);
		jmx.setName(file.getName());
		return jmx;
	}
}
