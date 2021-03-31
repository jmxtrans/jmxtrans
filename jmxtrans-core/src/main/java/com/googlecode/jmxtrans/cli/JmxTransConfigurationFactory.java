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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JmxTransConfigurationFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(JmxTransConfigurationFactory.class);

	public static JmxTransConfiguration fromProperties(File configFile) throws IOException {
		Properties properties = new Properties();
		File lConfigFile = null;
		// Default properties
		try (InputStream in = JmxTransConfiguration.class.getResourceAsStream("/jmxtrans.properties")) {
			properties.load(in);
		}
		// Property file
		if (configFile != null) {
			lConfigFile = configFile;
			try (InputStream in = new FileInputStream(configFile)) {
				properties.load(in);
			}
		} else {
			File defaultSystemProperties = new File("/etc/jmxtrans/jmxtrans.properties");
			if (defaultSystemProperties.isFile()) {
				lConfigFile = defaultSystemProperties;
				try (InputStream in = new FileInputStream(defaultSystemProperties)) {
					properties.load(in);
				}
			}
		}
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		if (lConfigFile != null) {
			LOGGER.info("Loading config file {}", lConfigFile);
		}
		configuration.loadProperties(properties);
		configuration.setConfigFile(lConfigFile);
		return configuration;
	}

	public static JmxTransConfiguration fromArgs(String... args) throws IOException {
		return new JCommanderArgumentParser().parseOptions(args);
	}
}
