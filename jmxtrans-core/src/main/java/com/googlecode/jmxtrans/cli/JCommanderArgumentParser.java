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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JCommanderArgumentParser implements CliArgumentParser {
	@Nonnull
	@Override
	public JmxTransConfiguration parseOptions(@Nonnull String[] args) throws IOException {
		JmxTransConfiguration tempConfig = new JmxTransConfiguration();
		JmxTransConfiguration configuration = new JmxTransConfiguration();
		new JCommander(tempConfig, args);

		JCommander jCommander = new JCommander();
		jCommander.setDefaultProvider(new FileConfiguration(localProperties(tempConfig.getConfigFile())));
		jCommander.addObject(configuration);
		jCommander.parse(args);

		if (configuration.isHelp()) jCommander.usage();
		else validate(configuration);

		return configuration;
	}

	private Properties localProperties(File configFile) throws IOException {
		Properties properties = new Properties(defaultProperties());
		File defaultSystemProperties = new File("/etc/jmxtrans/jmxtrans.properties");

		if (configFile != null) {
			try (InputStream in = new FileInputStream(configFile)) {
				properties.load(in);
			}
		} else if (defaultSystemProperties.isFile()) {
			try (InputStream in = new FileInputStream(defaultSystemProperties)) {
				properties.load(in);
			}
		}

		return properties;
	}

	private Properties defaultProperties() throws IOException {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("jmxtrans.properties")) {
			Properties properties = new Properties();
			properties.load(in);
			return properties;
		}
	}

	private void validate(JmxTransConfiguration configuration) {
		if (configuration.getProcessConfigDirOrFile() == null) throw new ParameterException("Please specify either the -f or -j option.");
	}

}
