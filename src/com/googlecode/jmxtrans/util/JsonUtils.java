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
